package edu.cuhk.csci3310.trackee;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailActivity extends AppCompatActivity {

    private final String sender_email="senderjoe03221@gmail.com";
    private final String sender_pw=String.valueOf(R.string.email_sender_pw);
    private final String receiver_email="receiverjoe03221@gmail.com";
    private TextView receiver_email_box;
    private EditText email_title;
    private EditText email_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email);
        //get element reference and init receiver
        receiver_email_box=findViewById(R.id.email_target);
        email_title=findViewById(R.id.email_title);
        email_content=findViewById(R.id.email_content);
        receiver_email_box.setText(receiver_email);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }


    public void sendEmail(View view){
        try{
            //google smtp mail server
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            //authenticate login session
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(sender_email, sender_pw);
                }
            });
            //message detail
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender_email));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver_email));
            message.setSubject(email_title.getText().toString());
            message.setText(email_content.getText().toString());
            //create a thread to handle async email sending
            //thread will auto stop after job done
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Transport.send(message);
                        //add a callback for toasting message and go back to main page
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_SHORT).show();
                                onBackPressed();
                            }
                        });

                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Sent Fail ,Pls check your network", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }).start();
        }catch(Exception e){
            e.printStackTrace();
        }


    }

    public void goBack(View view){
        onBackPressed();
    }

}
