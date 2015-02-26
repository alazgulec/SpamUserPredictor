package com.alazgulec.spamuserpretender;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.File;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;


public class QueryActivity extends ActionBarActivity {

    private TextView userNameLabel;
    private ImageView avatar;

    private Twitter twitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query);

        userNameLabel = (TextView) findViewById(R.id.userName);
        avatar = (ImageView) findViewById(R.id.avatar);

        Intent i = getIntent();
        String userName = i.getStringExtra("userName");
        userNameLabel.setText(userName);

        twitter = TwitterFactory.getSingleton();
        new RetrieveMetrics().execute(userName);

//        Twitter twitter = TwitterFactory.getSingleton();
//        List<Status> statuses = null;
//        try {
//            statuses = twitter.getHomeTimeline();
//        } catch (TwitterException te) {
//            System.out.println(te.toString());
//        }
//        System.out.println("Showing home timeline.");
//        for (Status status : statuses) {
//            System.out.println(status.getUser().getName() + ":" +
//                    status.getText());
//        }
    }


    private class RetrieveMetrics extends AsyncTask<String, Boolean, Boolean> {

        private double longevity;
        private double postedTweets;
        private int linkNumber;
        private double reputation;
        private String pictureUrl;
        private Bitmap bitmap;

        private static final String NAMESPACE = "http://10.0.2.2:8086/TwitterSpamDetector";
        private static final String URL ="http://10.0.2.2:8086/TwitterSpamDetector?wsdl";
        private static final String SOAP_ACTION = "http://10.0.2.2:8086/TwitterSpamDetector/predict";
        private static final String METHOD_NAME = "predict";

        @Override
        protected Boolean doInBackground(String... strings) {
            String userName = strings[0];

            User user = null;
            try {
                user = twitter.showUser(userName);
            } catch (TwitterException te) {
                System.out.println(te.toString());
            }

            pictureUrl = user.getBiggerProfileImageURL();
            longevity = (double) daysBetween(user.getCreatedAt());
            postedTweets = (double) user.getStatusesCount();
            linkNumber = 0;
            reputation = (double)user.getFollowersCount()/(user.getFriendsCount()+user.getFollowersCount());


            try {
                URL url = new URL(pictureUrl);
                //try this url = "http://0.tqn.com/d/webclipart/1/0/5/l/4/floral-icon-5.jpg"
                HttpGet httpRequest = null;

                httpRequest = new HttpGet(url.toURI());

                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = (HttpResponse) httpclient
                        .execute(httpRequest);

                HttpEntity entity = response.getEntity();
                BufferedHttpEntity b_entity = new BufferedHttpEntity(entity);
                InputStream input = b_entity.getContent();

                bitmap = BitmapFactory.decodeStream(input);

            } catch (Exception ex) {

            }

            Paging paging = new Paging(1, 100);
            List<twitter4j.Status> statuses = null;
            try {
                statuses = twitter.getUserTimeline(userName, paging);
            } catch (TwitterException te) {
                System.out.println(te.toString());
                return false;
            }

            for (twitter4j.Status status : statuses) {
//                String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
//                Pattern p = Pattern.compile(regex);
//                Matcher m = p.matcher(status.toString());
//                while (m.find()) {
//                    linkNumber++;
//                    String urlStr = m.group();
//                    if (urlStr.startsWith("(") && urlStr.endsWith(")")) {
//                        urlStr = urlStr.substring(1, urlStr.length() - 1);
//                    }
//                }

                if (status.getText().toLowerCase().contains("http://")){
                    linkNumber++;
                }
            }
//            SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
//            request.addProperty("arg0", String.valueOf(postedTweets));
//            request.addProperty("arg1", String.valueOf(longevity));
//            request.addProperty("arg2", String.valueOf((double) (linkNumber / 100)));
//            request.addProperty("arg3", String.valueOf(reputation));
//            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
//            envelope.setOutputSoapObject(request);
//            HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
//            try {
//                androidHttpTransport.call(SOAP_ACTION, envelope);
//
//                //SoapPrimitive  resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();
//                // SoapPrimitive  resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();
//                SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
//
//
//                System.out.println("Response: "+resultsRequestSOAP.toString());
//
//
//            } catch (Exception e) {
//
//                System.out.println("Error"+e);
//            }


            String result = predict(String.valueOf(postedTweets), String.valueOf(longevity), String.valueOf((double) (linkNumber / 100)), String.valueOf(reputation));
            System.out.println("Result: " + result);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean s) {
            if (s == false) {
                //Something went wrong
            } else {
                System.out.println("Longevity: " + String.valueOf(longevity));
                System.out.println("Posted Tweets: " + String.valueOf(postedTweets));
                System.out.println("Number of URLS: " + String.valueOf(linkNumber));
                System.out.println("Reputation: " + String.valueOf(reputation));
                avatar.setImageBitmap(bitmap);

            }
            super.onPostExecute(s);
        }

        protected int daysBetween(Date d1) {
            GregorianCalendar c = new GregorianCalendar();
            c.set(Calendar.HOUR_OF_DAY, 0); //anything 0 - 23
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            Date d2 = c.getTime();
            return (int) ((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
        }
    }


    public String predict(String numTweets, String longevity, String numUrlsPerTweet, String reputation){

        FastVector classValues = new FastVector();
        classValues.addElement("0");
        classValues.addElement("1");

        FastVector features = new FastVector();
        features.addElement(new Attribute("Posted Tweets"));
        features.addElement(new Attribute("Longevity"));
        features.addElement(new Attribute("#ofURLsPerTweet"));
        features.addElement(new Attribute("Reputation"));
        features.addElement(new Attribute("Spam", classValues));

        // predict instance class values
        Instances newData = new Instances("Test dataset", features, 0);

        double[] values = new double[newData.numAttributes()];
        values[0] = Double.parseDouble(numTweets);
        values[1] = Double.parseDouble(longevity);
        values[2] = Double.parseDouble(numUrlsPerTweet);
        values[3] = Double.parseDouble(reputation);
        values[4] = Instance.missingValue();

        newData.add(new Instance(1.0, values));
        newData.setClassIndex(4);

        //load model
        NaiveBayes classifier = null;
        try {
            File dir = Environment.getExternalStorageDirectory();
            File yourFile = new File(dir, "Download/twitter.model");
            String appPath = yourFile.getAbsolutePath();
            File file = new File(appPath);
            String filePath = file.getAbsolutePath();
            classifier = (NaiveBayes) SerializationHelper.read(filePath);
            System.out.println(classifier.toString());

            double myValue = classifier.classifyInstance(newData.instance(0));
            System.out.println("Predicted value is " + myValue);

            String prediction = newData.classAttribute().value((int) myValue);
            System.out.println("Predicted class is " + prediction);

            return prediction;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }

}
