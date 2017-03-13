/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.quickstart;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.*;
import com.google.firebase.quickstart.email.MyEmailer;
import com.google.firebase.quickstart.model.Post;
import com.google.firebase.quickstart.model.User;
import com.google.firebase.quickstart.servlet.MainServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.knowm.sundial.SundialJobScheduler;

import java.io.ByteArrayInputStream;

/**
 * Firebase Database quickstart sample for the Java Admin SDK.
 * See: https://firebase.google.com/docs/admin/setup#add_firebase_to_your_app
 */
public class Database {

    private static DatabaseReference database;

    /**
     * Notify a user of a new start and then update the last notification time.
     */
    private static void sendNotificationToUser(final String uid, final String postId) {
        final DatabaseReference userRef = database.child("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user.email != null) {
                    // Send email notification
                    MyEmailer.sendNotificationEmail(user.email, uid, postId);
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Unable to get user data from " + userRef.getKey());
                System.out.println("Error: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Update the startCount value to equal the number of stars in the map.
     */
    private static void updateStarCount(DatabaseReference postRef) {
        postRef.runTransaction(new Transaction.Handler() {
            public Transaction.Result doTransaction(MutableData mutableData) {
                Post post = mutableData.getValue(Post.class);
                if (post != null) {
                    // Update the starCount to be the same as the number of members in the stars map.
                    if (post.stars != null) {
                        post.starCount = post.stars.size();
                    } else {
                        post.starCount = 0;
                    }

                    mutableData.setValue(post);
                    return Transaction.success(mutableData);
                } else {
                    return Transaction.success(mutableData);
                }
            }

            public void onComplete(DatabaseError databaseError, boolean complete, DataSnapshot dataSnapshot) {
                System.out.println("updateStarCount:onComplete:" + complete);
            }
        });
    }

    /**
     * Start global listener for all Posts.
     */
    public static void startListeners() {
        database.child("posts").addChildEventListener(new ChildEventListener() {

            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildName) {
                final String postId = dataSnapshot.getKey();
                final Post post = dataSnapshot.getValue(Post.class);

                // Listen for changes in the number of stars and update starCount
                addStarsChangedListener(post, postId);

                // Listen for new stars on the post, notify users on changes
                addNewStarsListener(dataSnapshot.getRef(), post);
            }

            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildName) {}

            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildName) {}

            public void onCancelled(DatabaseError databaseError) {
                System.out.println("startListeners: unable to attach listener to posts");
                System.out.println("startListeners: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Listen for stars added or removed and update the starCount.
     */
    private static void addStarsChangedListener(Post post, String postId) {
        // Get references to the post in both locations
        final DatabaseReference postRef = database.child("posts").child(postId);
        final DatabaseReference userPostRef = database.child("user-posts").child(post.uid).child(postId);

        // When the post changes, update the star counts
        postRef.child("stars").addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                updateStarCount(postRef);
                updateStarCount(userPostRef);
            }

            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Unable to attach listener to stars for post: " + postRef.getKey());
                System.out.println("Error: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Send email to author when new star is received.
     */
    private static void addNewStarsListener(final DatabaseReference postRef, final Post post) {
        postRef.child("stars").addChildEventListener(new ChildEventListener() {
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildName) {
                // New star added, notify the author of the post
                sendNotificationToUser(post.uid, postRef.getKey());
            }

            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildName) {}

            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildName) {}

            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Unable to attach new star listener to: " + postRef.getKey());
                System.out.println("Error: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Send an email listing the top posts every Sunday.
     */
    private static void startEmailer() {
        SundialJobScheduler.startScheduler("com.google.firebase.quickstart.email");
    }

    private static void startWebServer() throws Exception {
        String port = System.getenv("PORT");
        if (port == null) port = "8080";
        Server server = new Server(Integer.valueOf(port));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MainServlet()),"/*");
        server.start();
    }

    public static void main(String[] args) throws Exception {
        startWebServer();

        // Initialize Firebase
        String serviceAccountJson = System.getenv("SERVICE_ACCOUNT_JSON");

        System.out.println(serviceAccountJson);

        String databaseUrl = System.getenv("DATABASE_URL");
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredential(FirebaseCredentials.fromCertificate(new ByteArrayInputStream(serviceAccountJson.getBytes())))
                .setDatabaseUrl(databaseUrl)
                .build();
        FirebaseApp.initializeApp(options);

        // Shared Database reference
        database = FirebaseDatabase.getInstance().getReference();

        // Start listening to the Database
        startListeners();

        // Kick off weekly email task
        startEmailer();
    }

}
