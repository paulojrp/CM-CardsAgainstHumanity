package com.example.asus.cardsagainsthumanity.game;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pConfig;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asus.cardsagainsthumanity.ManagerInterface;
import com.example.asus.cardsagainsthumanity.R;
import com.example.asus.cardsagainsthumanity.game.utils.AnswerArrayAdapter;
import com.example.asus.cardsagainsthumanity.game.utils.Game;
import com.example.asus.cardsagainsthumanity.router.AllEncompasingP2PClient;
import com.example.asus.cardsagainsthumanity.router.MeshNetworkManager;
import com.example.asus.cardsagainsthumanity.router.Packet;
import com.example.asus.cardsagainsthumanity.router.Receiver;
import com.example.asus.cardsagainsthumanity.router.Sender;
import com.example.asus.cardsagainsthumanity.wifi.WifiDirectBroadcastReceiver;

import java.util.ArrayList;

public class PlayerPick extends AppCompatActivity implements ManagerInterface
{
    private ArrayList<String> playerNames;
    private ArrayList<Integer> playerPoints;
    private AnswerArrayAdapter adapter;
    private int maxCards = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_pick);

        Receiver.setActivity(this);

        // initializeGame(savedInstanceState);

        Button button = (Button) findViewById(R.id.show_dialog_box);
        button.setText(Game.deviceName + " - " + Game.scoreTable.get(Game.deviceName) + " pts");

        TextView textView = (TextView) findViewById(R.id.textRound);
        textView.setText("Round "+Game.roundNumber);

        TextView questionTextView = (TextView) findViewById(R.id.black_card);
        String[] blackCardText = Game.getBlackCardText(Game.questionID);
        questionTextView.setText("[" + blackCardText[1] + "] " + blackCardText[0]);
        Game.numAnswers = Integer.parseInt(blackCardText[1]);

        playerNames = new ArrayList<String>(Game.scoreTable.keySet());
        playerPoints = new ArrayList<Integer>(Game.scoreTable.values());

        final ListView listView = (ListView) findViewById(R.id.answerList);
        int[] ids = Game.getWhiteCardId(maxCards);
        String[] values = new String[maxCards];
        for(int i=0; i<maxCards; i++){
            values[i] = Integer.toString(ids[i]);
        }

        adapter = new AnswerArrayAdapter(this, values, Game.numAnswers);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.itemClicked(position);

                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                if(adapter.getClickedItensSize() == Game.numAnswers) {
                    fab.setClickable(true);
                } else {
                    fab.setClickable(false);
                }
            }
        });
    }

    public void openScoreTable(View view) {
        ScoreTable scoreTable = ScoreTable.newInstance(playerNames, playerPoints);
        scoreTable.show(getFragmentManager(), "ScoreTableFragment");
    }

    @Override
    public String getActivityName()
    {
        return "PlayerPick";
    }

    public void sendAnswer(View view) {
        ArrayList<String> arrayList = adapter.getClickedItens();

        ArrayList<Integer> answers = new ArrayList<>();
        String whiteCardMessage = arrayList.get(0);
        answers.add(Integer.parseInt(arrayList.get(0)));
        if(arrayList.size() > 1) {
            whiteCardMessage += ",";
            whiteCardMessage += arrayList.get(1);
            answers.add(Integer.parseInt(arrayList.get(1)));
        }

        for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values())
        {
            if (c.getMac().equals(MeshNetworkManager.getSelf().getMac()))
            {
                Game.responsesID.add(answers);
                continue;
            }
            Log.wtf("SENDING WHITE CARD TO: ", " " + c.getMac());
            Sender.queuePacket(new Packet(Packet.TYPE.WHITECARD, whiteCardMessage.getBytes(), c.getMac(),
                    WifiDirectBroadcastReceiver.MAC));
        }

        Intent intent = new Intent(PlayerPick.this, PlayerWait.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("Question", Game.questionID);
        intent.putExtra("RoundNumber", Game.roundNumber);
        intent.putExtra("isCzar", Game.isCzar);
        intent.putExtra("numAnswers", Game.numAnswers);
        startActivity(intent);
        finish();
    }

    private void initializeGame(Bundle savedInstanceState) {
        int question, round, numAnswers;
        boolean isCzar;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                question = -1;
                round = -1;
                isCzar = false;
                numAnswers = -1;
            } else {
                question = extras.getInt("Question");
                round = extras.getInt("RoundNumber");
                isCzar = extras.getBoolean("isCzar");
                numAnswers = extras.getInt("numAnswers");
            }
        } else {
            question = (int) savedInstanceState.getSerializable("Question");
            round = (int) savedInstanceState.getSerializable("RoundNumber");
            isCzar = (boolean) savedInstanceState.getSerializable("isCzar");
            numAnswers = (int) savedInstanceState.getSerializable("numAnswers");
        }
        Game.questionID = question;
        Game.roundNumber = round;
        Game.isCzar = isCzar;
        Game.numAnswers = numAnswers;
    }

    @Override
    public void onBackPressed() {
        // User used back but nothing happened
    }
}
