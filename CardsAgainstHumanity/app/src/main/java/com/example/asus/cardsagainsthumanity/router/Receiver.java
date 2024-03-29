package com.example.asus.cardsagainsthumanity.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// import com.ecse414.android.echo.MessageActivity;
// import com.ecse414.android.echo.WiFiDirectActivity;
import com.example.asus.cardsagainsthumanity.ManagerInterface;
import com.example.asus.cardsagainsthumanity.RoomActivity;
// import com.example.asus.cardsagainsthumanity.RoomPeersList;
import com.example.asus.cardsagainsthumanity.config.Configuration;
import com.example.asus.cardsagainsthumanity.game.CzarPick;
import com.example.asus.cardsagainsthumanity.game.FinalGame;
import com.example.asus.cardsagainsthumanity.game.FinalRound;
import com.example.asus.cardsagainsthumanity.game.PlayerPick;
import com.example.asus.cardsagainsthumanity.game.PlayerWait;
import com.example.asus.cardsagainsthumanity.game.utils.Game;
import com.example.asus.cardsagainsthumanity.router.tcp.TcpReciever;
// import com.ecse414.android.echo.ui.DeviceDetailFragment;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * The main receiver class
 * @author Matthew Vertescher
 * @author Peter Henderson
 */
public class Receiver implements Runnable {

	/**
	 * Flag if the receiver has been running to prevent overzealous thread spawning
	 */
	public static boolean running = false;
	
	/**
	 * A ref to the activity
	 */
	static Activity activity;

	/**
	 * Constructor with activity
	 * @param a
	 */
	public Receiver(Activity a) {
		Receiver.activity = a;
		running = true;
	}

	/** 
	 * Main thread runner
	 */
	public void run() {
		/*
		 * A queue for received packets
		 */
		ConcurrentLinkedQueue<Packet> packetQueue = new ConcurrentLinkedQueue<Packet>();

		Log.wtf("RECEIVER", "OPENED");
		/*
		 * Receiver thread 
		 */
		new Thread(new TcpReciever(Configuration.RECEIVE_PORT, packetQueue)).start();

		Packet p;

		/*
		 * Keep going through packets
		 */
		while (true)
		{
			/*
			 * If the queue is empty, sleep to give up CPU cycles
			 */
			while (packetQueue.isEmpty())
			{
				try
				{
					Thread.sleep(125);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			/*
			 * Pop a packet off the queue
			 */
			p = packetQueue.remove();

			Log.wtf("PACKET RECEIVED", "RECEIVED");
			/*
			 * If it's a hello, this is special and need to go through the connection mechanism for any node receiving this
			 */
			if (p.getType().equals(Packet.TYPE.HELLO))
			{
				// Put it in your routing table
				for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values())
				{
					if (c.getMac().equals(MeshNetworkManager.getSelf().getMac()) || c.getMac().equals(p.getSenderMac()))
					{
						continue;
					}
					Packet update = new Packet(Packet.TYPE.UPDATE, Packet.getMacAsBytes(p.getSenderMac()), c.getMac(),
							MeshNetworkManager.getSelf().getMac());
					Sender.queuePacket(update);
				}

				MeshNetworkManager.routingTable.put(p.getSenderMac(),
						new AllEncompasingP2PClient(p.getSenderMac(), p.getSenderIP(), p.getSenderMac(),
								MeshNetworkManager.getSelf().getMac()));

				// Send routing table back as HELLO_ACK
				byte[] rtable = MeshNetworkManager.serializeRoutingTable();

				Packet ack = new Packet(Packet.TYPE.HELLO_ACK, rtable, p.getSenderMac(), MeshNetworkManager.getSelf()
						.getMac());
				Sender.queuePacket(ack);
                Game.isCzar = true;
				somebodyJoined(p.getSenderMac());
				updatePeerList();
			}
            else if (p.getType().equals(Packet.TYPE.BYE))
            {
				MeshNetworkManager.routingTable.remove(p.getSenderMac());
				Receiver.somebodyLeft(p.getSenderMac());
				Receiver.updatePeerList();
			}
            else
            {
				// If you're the intended target for a non hello message
				if (p.getMac().equals(MeshNetworkManager.getSelf().getMac()))
                {
					//if we get a hello ack populate the table
					if (p.getType().equals(Packet.TYPE.HELLO_ACK))
                    {
						MeshNetworkManager.deserializeRoutingTableAndAdd(p.getData());
						MeshNetworkManager.getSelf().setGroupOwnerMac(p.getSenderMac());
                        Game.isCzar = false;
						somebodyJoined(p.getSenderMac());
						updatePeerList();
					}
                    else if (p.getType().equals(Packet.TYPE.UPDATE))
                    {
						//if it's an update, add to the table
						String emb_mac = Packet.getMacBytesAsString(p.getData(), 0);
						MeshNetworkManager.routingTable.put(emb_mac,
								new AllEncompasingP2PClient(emb_mac, p.getSenderIP(), p.getMac(), MeshNetworkManager
										.getSelf().getMac()));

						final String message = emb_mac + " joined the conversation";
						final String name = p.getSenderMac();
						/*activity.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if (activity.isVisible) {
									Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
								} else {
									MessageActivity.addMessage(name, message);
								}
							}
						});*/
						updatePeerList();
					}
                    else if (p.getType().equals(Packet.TYPE.MESSAGE))
                    {
						//If it's a message display the message and update the table if they're not there 
						// for whatever reason
						final String message = p.getSenderMac() + " says:\n" + new String(p.getData());
						final String msg = new String(p.getData());
						final String name = p.getSenderMac();

						if (!MeshNetworkManager.routingTable.contains(p.getSenderMac()))
                        {
							/*
							 * Update your routing table if for some reason this
							 * guy isn't in it
							 */
							MeshNetworkManager.routingTable.put(p.getSenderMac(),
									new AllEncompasingP2PClient(p.getSenderMac(), p.getSenderIP(), p.getSenderMac(),
											MeshNetworkManager.getSelf().getGroupOwnerMac()));
						}
						/*activity.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if (activity.isVisible) {
									Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
								} else {
									MessageActivity.addMessage(name, msg);
								}
							}
						});*/
						updatePeerList();
					}
                    else if (p.getType().equals(Packet.TYPE.CZAR) && ("RoomActivity".equals(((ManagerInterface) activity).getActivityName()) ||
                            ("FinalRound".equals(((ManagerInterface) activity).getActivityName()))))
                    {
                        String data = new String(p.getData());
                        String[] separated = data.split(",");
                        if (MeshNetworkManager.getSelf().getMac().equals(separated[0]))  // Check if user was delegated to be CZAR
                        {
                            Game.isCzar = true;
							Game.questionID = Integer.parseInt(separated[1]);
							String[] questionText = Game.getBlackCardText(Integer.parseInt(separated[1]));  //[0] has text, [1] has number of answers
							Game.numAnswers = Integer.parseInt(questionText[1]);

							Intent intent = new Intent(activity, CzarPick.class);
							intent.putExtra("Question", Game.questionID);
							intent.putExtra("RoundNumber", Game.roundNumber);
							intent.putExtra("isCzar", Game.isCzar);
							intent.putExtra("numAnswers", Game.numAnswers);
							activity.startActivity(intent);
							if ("FinalRound".equals(((ManagerInterface) activity).getActivityName()))
								activity.finish();
                        }
                        else
                        {
                            Game.isCzar = false;
							Game.questionID = Integer.parseInt(separated[1]);
							if("RoomActivity".equals(((ManagerInterface) activity).getActivityName()))
							{
								TreeMap<String, Integer> hashResults = new TreeMap<String, Integer>();
								for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values()) {
									hashResults.put(c.getMac(), 0);
								}
								Game.scoreTable = Game.sortByValue(hashResults);
							}
							Game.deviceName = MeshNetworkManager.getSelf().getMac();
							Game.isCzar = false;

							Intent intent = new Intent(activity, PlayerPick.class);
							// intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							intent.putExtra("Question", Game.questionID);
							intent.putExtra("RoundNumber", Game.roundNumber);
							intent.putExtra("isCzar", Game.isCzar);
							activity.startActivity(intent);
							if ("FinalRound".equals(((ManagerInterface) activity).getActivityName()))
								activity.finish();
                        }
                    }
					else if (p.getType().equals(Packet.TYPE.WHITECARD)) {
						final String data = new String(p.getData());
						final String senderMacAddress = p.getSenderMac();
						Log.wtf("White Card Received: ", " " + data);
						if (("PlayerWait".equals(((ManagerInterface) activity).getActivityName()))) {
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									((PlayerWait) activity).addPlayerResponse(data);
								}
							});
						} else if (("CzarPick".equals(((ManagerInterface) activity).getActivityName()))) {
							Log.d("PACKET WHITECARD", data);
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Log.d("PACKET", data);
									((CzarPick) activity).addPlayerResponse(data, senderMacAddress);
								}
							});
						} else if (("PlayerPick".equals(((ManagerInterface) activity).getActivityName()))) {
							ArrayList<Integer> arrayList = new ArrayList<>();
							String[] parseData = data.split(",");
							for (String tempString : parseData) {
								arrayList.add(Integer.parseInt(tempString));
							}
							Game.responsesID.add(arrayList);
						}
					}
                    else if (p.getType().equals(Packet.TYPE.WINNER) && (("PlayerWait".equals(((ManagerInterface) activity).getActivityName()))
                    || ("PlayerPick".equals(((ManagerInterface) activity).getActivityName()))))
                    {
                        String data = new String(p.getData());
                        String[] separated = data.split(";");

						if(Game.scoreTable.containsKey(separated[0])){
							Game.scoreTable.put(separated[0], Game.scoreTable.get(separated[0]) + 1);
							Game.scoreTable = Game.sortByValue(Game.scoreTable);

							if(Game.scoreTable.get(separated[0]) >= 5) {
								Intent intent = new Intent(activity, FinalGame.class);
								intent.putExtra("winnerMac", separated[0]);
								if(Game.numAnswers==1)
									intent.putExtra("winnerCardsID", separated[1]);
								else
									intent.putExtra("winnerCardsID", separated[1]+","+separated[2]);
								activity.startActivity(intent);
								activity.finish();
								return;
							}
						}

                        Intent intent = new Intent(activity, FinalRound.class);
                        intent.putExtra("winnerMac", separated[0]);
						if(separated.length == 2)
                        	intent.putExtra("winnerCardsID", separated[1]);
						else
							intent.putExtra("winnerCardsID", separated[1]+","+separated[2]);
						activity.startActivity(intent);
						activity.finish();
                    }
				}
                else
                {
					// otherwise forward it if you're not the recipient
					int ttl = p.getTtl();
					// Have a ttl so that they don't bounce around forever
					ttl--;
					if (ttl > 0)
                    {
						Sender.queuePacket(p);
						p.setTtl(ttl);
					}
				}
			}

		}
	}

	/**
	 * GUI thread to send somebody joined notification
	 * @param smac
	 */
	public static void somebodyJoined(String smac) {

		final String message;
		final String msg;
		message = msg = smac + " has joined.";
		final String name = smac;
		/*activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (activity.isVisible) {
					Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
				} else {
					MessageActivity.addMessage(name, msg);
				}
			}
		});*/
	}

	/**
	 * Somebody left notification on the UI thread
	 * @param smac
	 */
	public static void somebodyLeft(String smac) {

		final String message;
		final String msg;
		message = msg = smac + " has left.";
		final String name = smac;
		/*activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (activity.isVisible) {
					Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
				} else {
					MessageActivity.addMessage(name, msg);
				}
			}
		});*/
	}

	/**
	 * Update the list of peers on the front page
	 */
	public static void updatePeerList() {
		if (activity == null)
			return;

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if( "RoomActivity".equals(((ManagerInterface) activity).getActivityName()))
					((RoomActivity) activity).updatePeersList();
			}
		});
	}

	public static void setActivity(Activity activity) {
		Receiver.activity = activity;
	}
}