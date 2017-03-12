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
package com.google.firebase.quickstart.email;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to send email notifications from the server.
 */
public class MyEmailer {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient client = new OkHttpClient();

    public static void sendNotificationEmail(String emailAddr, String uid, String postId) {
        System.out.println("sendNotificationEmail: " + emailAddr);

        try {
            String smtpServer = System.getenv("MAILGUN_SMTP_SERVER");
            if (smtpServer != null) {
//                String url = "https://api:" + apiKey + "@api.mailgun.net/v2/" + domain + "/messages";
//
//                String json = "{" +
//                    "'from':'test@example.com'," +
//                    "'to': '" + email + "'," +
//                    "'subject': 'New post!'," +
//                    "'text': 'This is a notfication from your Heroku app. There was a new post received by Firebase.'" +
//                    "}";
//
//                RequestBody body = RequestBody.create(JSON, json);
//                Request request = new Request.Builder()
//                    .url(url)
//                    .post(body)
//                    .build();
//                Response response = client.newCall(request).execute();
//                System.out.println("[mailgun] " + response.message());
//                System.out.println("[mailgun] " + response.code());
//                System.out.println("[mailgun] " + response.body());

                Integer smtpPort = Integer.valueOf(System.getenv("MAILGUN_SMTP_PORT"));
                String smtpUsername = System.getenv("MAILGUN_SMTP_LOGIN");
                String smtpPassword = System.getenv("MAILGUN_SMTP_PASSWORD");

                Email email = new SimpleEmail();
                email.setHostName(smtpServer);
                email.setSmtpPort(smtpPort);
                email.setAuthenticator(new DefaultAuthenticator(smtpUsername, smtpPassword));
                email.setSSLOnConnect(true);
                email.setFrom("test@example.com");
                email.setSubject("New post!");
                email.setMsg("This is a notfication from your Heroku app. There was a new post received by Firebase.");
                email.addTo(emailAddr);
                String result = email.send();

                System.out.println("[mailgun] " + result);

                // Save the date of the last notification sent
                Map<String, Object> update = new HashMap<String, Object>();
                update.put("/posts/" + postId + "/lastNotificationTimestamp", ServerValue.TIMESTAMP);
                update.put("/user-posts/" + uid + "/" + postId + "/lastNotificationTimestamp", ServerValue.TIMESTAMP);

                FirebaseDatabase.getInstance().getReference().updateChildren(update);
            }
        } catch (Exception ex) {
            System.out.println("Whoops! Unable to send email.");
            ex.printStackTrace();
        }
    }

}
