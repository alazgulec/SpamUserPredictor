package com.alazgulec.spamuserpretender;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;


import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
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
    private BarChart chart;
    private BarChart chart2;
    private BarChart chart3;
    private TextView text1;
    private TextView text2;
    private TextView text3;
    private TextView text4;
    private TextView text5;
    private Dialog loadingDialog;

    private Twitter twitter;
    private String result;
    String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_query);

        userNameLabel = (TextView) findViewById(R.id.userName);
        avatar = (ImageView) findViewById(R.id.avatar);
        chart = (BarChart) findViewById(R.id.barchart);
        chart2 = (BarChart) findViewById(R.id.barchart2);
        chart3 = (BarChart) findViewById(R.id.barchart3);
        text1 = (TextView) findViewById(R.id.text1);
        text2 = (TextView) findViewById(R.id.text2);
        text3 = (TextView) findViewById(R.id.text3);
        text4 = (TextView) findViewById(R.id.text4);
        text5 = (TextView) findViewById(R.id.text5);

        Intent i = getIntent();
        userName = i.getStringExtra("userName");
        userNameLabel.setText(userName);

        twitter = TwitterFactory.getSingleton();
        loadingDialog = ProgressDialog.show(QueryActivity.this, "Twitter Spam Detector",
                "Loading. Please wait...", true);
        new RetrieveMetrics().execute(userName);

    }


    private class RetrieveMetrics extends AsyncTask<String, Boolean, Boolean> {

        private double longevity;
        private double postedTweets;
        private int linkNumber;
        private double reputation;
        private String pictureUrl;
        private Bitmap bitmap;

        private double selfLongevity;
        private double selfPostedTweets;
        private int selfLinkNumber;
        private double selfReputation;
        private User selfUser;

        @Override
        protected Boolean doInBackground(String... strings) {
            String userName = strings[0];

            User user = null;
            selfUser = null;

            try {
                long id = twitter.getId();
                selfUser = twitter.showUser(id);
                System.out.println("userName: " + selfUser.getScreenName());
                user = twitter.showUser(userName);
            } catch (TwitterException te) {
                System.out.println(te.toString());
                return false;
            }

            pictureUrl = user.getBiggerProfileImageURL();
            longevity = (double) daysBetween(user.getCreatedAt());
            postedTweets = (double) user.getStatusesCount();
            linkNumber = 0;
            reputation = (double)user.getFollowersCount()/(user.getFriendsCount()+user.getFollowersCount());


            selfLongevity = (double) daysBetween(selfUser.getCreatedAt());
            selfPostedTweets = (double) selfUser.getStatusesCount();
            selfLinkNumber = 0;
            selfReputation = (double)selfUser.getFollowersCount()/(selfUser.getFriendsCount()+selfUser.getFollowersCount());


            try {
                URL url = new URL(pictureUrl);
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
            List<twitter4j.Status> selfStatuses = null;
            try {
                statuses = twitter.getUserTimeline(userName, paging);
                selfStatuses = twitter.getUserTimeline(selfUser.getId(), paging);
            } catch (TwitterException te) {
                System.out.println(te.toString());
                return false;
            }

            for (twitter4j.Status status : statuses) {
                if (status.getText().toLowerCase().contains("http://")){
                    linkNumber++;
                }
            }

            for (twitter4j.Status status : selfStatuses) {
                if (status.getText().toLowerCase().contains("http://")){
                    selfLinkNumber++;
                }
            }

            result = predict(String.valueOf(postedTweets), String.valueOf(longevity), String.valueOf((double) linkNumber / 100), String.valueOf(reputation));
            System.out.println("Result: " + result);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean s) {
            if (s == false) {
                Toast.makeText(getApplicationContext(), "No user has found!", Toast.LENGTH_LONG).show();
            } else {
                System.out.println("Longevity: " + String.valueOf(longevity));
                System.out.println("Posted Tweets: " + String.valueOf(postedTweets));
                System.out.println("Number of URLS: " + String.valueOf((double) linkNumber / 100));
                System.out.println("Reputation: " + String.valueOf(reputation));
                text1.setText("Longevity: " + String.valueOf(longevity) + " days");
                text2.setText("Tweet #: " + String.valueOf(postedTweets));
                text3.setText("# of URLS per tweet: " + String.valueOf((double) linkNumber / 100));
                text4.setText("Rep: " + String.valueOf(reputation));
                if(result.equals("1")) {
                    Context context = getApplicationContext();
                    Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(1500);
                    text5.setText("SPAM!");
                    text5.setTextColor(Color.RED);
                } else {
                    text5.setText("LEGIT!");
                    text5.setTextColor(Color.GREEN);
                }

                avatar.setImageBitmap(bitmap);

                ArrayList<BarEntry> setOne = new ArrayList<BarEntry>();
                BarEntry l1u1 = new BarEntry((float) longevity, 0);
                BarEntry l1u2 = new BarEntry((float) selfLongevity, 1);
                setOne.add(l1u1);
                setOne.add(l1u2);
                ArrayList<BarEntry> setTwo = new ArrayList<BarEntry>();
                BarEntry p1u1 = new BarEntry((float) postedTweets, 0);
                BarEntry p1u2 = new BarEntry((float) selfPostedTweets, 1);
                setTwo.add(p1u1);
                setTwo.add(p1u2);
                ArrayList<BarEntry> setThree = new ArrayList<BarEntry>();
                BarEntry n1u1 = new BarEntry((float) linkNumber/100, 0);
                BarEntry n1u2 = new BarEntry((float) selfLinkNumber/100, 1);
                setThree.add(n1u1);
                setThree.add(n1u2);
                ArrayList<BarEntry> setFour = new ArrayList<BarEntry>();
                BarEntry r1u1 = new BarEntry((float) reputation, 0);
                BarEntry r1u2 = new BarEntry((float) selfReputation, 1);
                setFour.add(r1u1);
                setFour.add(r1u2);

                BarDataSet barDataSet1 = new BarDataSet(setOne, "Longevity");
                BarDataSet barDataSet2 = new BarDataSet(setTwo, "Posted Tweets");
                BarDataSet barDataSet3 = new BarDataSet(setThree, "Number of URLs");
                BarDataSet barDataSet4 = new BarDataSet(setFour, "Reputation");
                ArrayList<BarDataSet> dataSetArray = new ArrayList<BarDataSet>();

                barDataSet3.setColor(Color.GREEN);
                dataSetArray.add(barDataSet3);
                dataSetArray.add(barDataSet4);

                ArrayList<String> titleArray = new ArrayList<String>();
                titleArray.add(userName);
                titleArray.add(selfUser.getScreenName());

                BarData barData = new BarData(titleArray, barDataSet1);
                BarData barData2 = new BarData(titleArray, barDataSet2);
                BarData barData3 = new BarData(titleArray, dataSetArray);

                chart.setDrawValueAboveBar(true);
                chart.setDrawGridBackground(false);
                chart.setDrawBarShadow(false);
                chart.setDescription("Longevity in days");
                chart.setNoDataTextDescription("");
                chart.setData(barData);

                chart2.setData(barData2);
                chart2.setDescription("");

                chart3.setData(barData3);
                chart3.setDescription("");
            }
            loadingDialog.dismiss();
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
