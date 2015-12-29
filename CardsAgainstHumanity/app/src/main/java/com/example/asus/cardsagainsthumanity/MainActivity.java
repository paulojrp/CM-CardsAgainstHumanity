package com.example.asus.cardsagainsthumanity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    private WifiP2pManager manager;
    private final IntentFilter intentFilter = new IntentFilter();

    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;

    private WifiManager wifiManager;
    private IntentFilter wifiIntentFilter = new IntentFilter();
    private BroadcastReceiver wifiReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set peer2peer actions
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener()
        {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group)
            {
                if (group != null)
                {
                    manager.removeGroup(channel, new WifiP2pManager.ActionListener()
                    {
                        @Override
                        public void onSuccess()
                        {
                            System.out.println("Success");
                        }

                        @Override
                        public void onFailure(int reason)
                        {
                            System.out.println("Failure " + reason);
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void createGame(View view)
    {
        manager.createGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d("Create Game: ", "P2P Group created");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("Create Game: ", "P2P Group failed");
            }
        });

        Intent intent = new Intent(this, RoomActivity.class);
        startActivity(intent);
    }

    public void joinGame(View view)
    {

    }
}
