/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd.service.firestore;

import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gnd.rx.RxTask;
import com.google.android.gnd.service.DatastoreEvent;
import com.google.android.gnd.service.RemoteDataService;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Timestamps;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.SnapshotMetadata;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirestoreDataService implements RemoteDataService {

  private static final FirebaseFirestoreSettings FIRESTORE_SETTINGS =
      new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build();
  private static final SetOptions MERGE = SetOptions.merge();
  private static final String TAG = FirestoreDataService.class.getSimpleName();
  private final GndFirestore db;

  @Inject
  FirestoreDataService() {
    // TODO: Run on I/O thread, return asynchronously.
    final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    firestore.setFirestoreSettings(FIRESTORE_SETTINGS);
    FirebaseFirestore.setLoggingEnabled(true);
    db = new GndFirestore(firestore);
  }

  static Timestamps toTimestamps(@Nullable Date created, @Nullable Date modified) {
    Timestamps.Builder timestamps = Timestamps.newBuilder();
    if (created != null) {
      timestamps.setCreated(created);
    }
    if (modified != null) {
      timestamps.setModified(modified);
    }
    return timestamps.build();
  }

  @Override
  public Single<Project> loadProject(String projectId) {
    return db.project(projectId)
        .getDocument()
        .map(ProjectDoc::toProto)
        .switchIfEmpty(Single.error(new DocumentNotFoundException()));
  }

  @Override
  public Single<List<Record>> loadRecordSummaries(Place place) {
    return toSingleList(
        db.project(place.getProject().getId()).records().getByFeatureId(place.getId()),
        doc -> RecordDoc.toProto(place, doc.getId(), doc));
  }

  @Override
  public Single<Record> loadRecordDetails(Place place, String recordId) {
    return db.project(place.getProject().getId())
        .records()
        .record(recordId)
        .getDocument()
        .map(doc -> RecordDoc.toProto(place, doc.getId(), doc))
        .toSingle();
  }

  @Override
  public Single<List<Project>> loadProjectSummaries(User user) {
    return toSingleList(db.projects().getReadable(user), ProjectDoc::toProto);
  }

  @Override
  public Flowable<DatastoreEvent<Place>> getPlaceVectorStream(Project project) {
    return db.project(project.getId())
        .places()
        .getFlowable()
        .flatMapIterable(
            placeQuerySnapshot ->
                toDatastoreEvents(
                    placeQuerySnapshot,
                    placeDocSnapshot -> PlaceDoc.toProto(project, placeDocSnapshot)))
        .doOnTerminate(
            () ->
                Log.d(
                    TAG,
                    "getPlaceVectorStream stream for project " + project.getId() + " terminated."));
  }

  private <T> Iterable<DatastoreEvent<T>> toDatastoreEvents(
      QuerySnapshot snapshot, Function<DocumentSnapshot, T> converter) {
    DatastoreEvent.Source source = getSource(snapshot.getMetadata());
    return stream(snapshot.getDocumentChanges())
        .map(dc -> toDatastoreEvent(dc, source, converter))
        .filter(DatastoreEvent::isValid)
        .collect(toList());
  }

  private <T> DatastoreEvent<T> toDatastoreEvent(
      DocumentChange dc, DatastoreEvent.Source source, Function<DocumentSnapshot, T> converter) {
    Log.v(TAG, toString(dc));
    try {
      String id = dc.getDocument().getId();
      switch (dc.getType()) {
        case ADDED:
          return DatastoreEvent.loaded(id, source, converter.apply(dc.getDocument()));
        case MODIFIED:
          return DatastoreEvent.modified(id, source, converter.apply(dc.getDocument()));
        case REMOVED:
          return DatastoreEvent.removed(id, source);
      }
    } catch (DatastoreException e) {
      Log.d(TAG, "Datastore error:", e);
    }
    return DatastoreEvent.invalidResponse();
  }

  @NonNull
  private static String toString(DocumentChange dc) {
    return dc.getDocument().getReference().getPath() + " " + dc.getType();
  }

  private static DatastoreEvent.Source getSource(SnapshotMetadata metadata) {
    return metadata.hasPendingWrites()
        ? DatastoreEvent.Source.LOCAL_DATASTORE
        : DatastoreEvent.Source.REMOTE_DATASTORE;
  }

  // TODO: Move relevant Record fields and updates into "RecordUpdate" object.
  @Override
  public Single<Record> saveChanges(Record record, ImmutableList<ValueUpdate> updates) {
    GndFirestore.RecordsRef records = db.projects().project(record.getProject().getId()).records();

    if (record.getId() == null) {
      DocumentReference recordDocRef = records.ref().document();
      record = record.toBuilder().setId(recordDocRef.getId()).build();
      return saveChanges(recordDocRef, record, updates);
    } else {
      return saveChanges(records.record(record.getId()).ref(), record, updates);
    }
  }

  private Single<Record> saveChanges(
      DocumentReference recordDocRef, Record record, ImmutableList<ValueUpdate> updates) {
    return RxTask.toCompletable(
            () ->
                db.batch()
                    .set(recordDocRef, RecordDoc.forUpdates(record, updatedValues(updates)), MERGE)
                    .commit())
        .andThen(Single.just(record));
  }

  @Override
  public Single<Place> addPlace(Place place) {
    return db.project(place.getProject().getId())
        .places()
        .add(PlaceDoc.fromProto(place))
        .map(docRef -> place.toBuilder().setId(docRef.getId()).build());
  }

  private Map<String, Object> updatedValues(ImmutableList<ValueUpdate> updates) {
    Map<String, Object> updatedValues = new HashMap<>();
    for (ValueUpdate valueUpdate : updates) {
      switch (valueUpdate.getOperation()) {
        case CREATE:
        case UPDATE:
          valueUpdate
              .getValue()
              .ifPresent(
                  value ->
                      updatedValues.put(valueUpdate.getElementId(), RecordDoc.toObject(value)));
          break;
        case DELETE:
          // FieldValue.delete() is not working in nested objects; if it doesn't work in the future
          // we can remove them using dot notation ("responses.{elementId}").
          updatedValues.put(valueUpdate.getElementId(), "");
          break;
      }
    }
    return updatedValues;
  }

  /**
   * Applies the provided mapping function to each document in the specified query snapshot, if
   * present. If no results are present, completes with an empty list.
   */
  private static <T> Single<List<T>> toSingleList(
      Maybe<QuerySnapshot> result, Function<DocumentSnapshot, T> mappingFunction) {
    return result
        .map(
            querySnapshot ->
                stream(querySnapshot.getDocuments())
                    .map(mappingFunction)
                    .collect(Collectors.toList()))
        .toSingle(Collections.emptyList());
  }
}
