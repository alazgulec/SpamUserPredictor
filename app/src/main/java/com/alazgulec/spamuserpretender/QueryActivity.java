package com.alazgulec.spamuserpretender;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;


public class QueryActivity extends ActionBarActivity {

    private TextView userNameLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query);

        userNameLabel = (TextView) findViewById(R.id.userName);

        Intent i= getIntent();
        String userName = i.getStringExtra("userName");
        userNameLabel.setText(userName);

        Twitter twitter = TwitterFactory.getSingleton();
        User user = null;
        try {
             user = twitter.showUser("jack");
        } catch (TwitterException te) {
            System.out.println(te.toString());
        }
        System.out.println("Number of Followers: " + user.getFollowersCount());
        System.out.println("Number of Following: " + user.getFriendsCount());
        //user.getCreatedAt();
        Paging paging = new Paging(1, 100);
        List<Status> statuses = null;
        try {
            statuses = twitter.getUserTimeline("jack", paging);
        } catch (TwitterException te) {
            System.out.println(te.toString());
        }
//      List<Status> statusesFav = twitter.getFavorites("jack", paging);
//      List<User> users = twitter.getFollowersList("jack", 50);
        System.out.println("Showing home timeline.");
        for (Status status : statuses) {
            System.out.println(status.getUser().getName() + ":" +
                    status.getText() + " " + status.getRetweetCount()) ;
        }

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

}
