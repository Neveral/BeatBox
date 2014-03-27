import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.event.*;

public class BeatBox {
	JPanel mainPanel;
	JFrame theFrame;
	JList incomingList;
	JTextField userMessage;
	ArrayList<JCheckBox> checkboxList;
	int nextNum;
	Vector<String> listVector = new Vector<String>();
	String userName;
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
	
	Sequencer sequencer;
	Sequence sequence;
	Sequence mySequence = null;
	Track track;
	
	
	String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", 		"Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand 		Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low 		Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", 		"Open Hi Conga"};
	int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};
	
	public static void main(String[] args) {
		new BeatBox().startUp("name");
	}
	
	public void startUp(String name) {
		userName = name;
		// открыаем соединение с сервером
		try {
			Socket sock = new Socket("127.0.0.1", 4242);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			
			Thread remote = new Thread(new RemoteReader());
			remote.start();
			
		}catch(Exception ex) {
			System.out.println("coldn't connect - you'll have to play alone");
		}
		buildGUI();
		setUpMidi();
		
	}
	
	public void buildGUI() {
		theFrame = new JFrame("BeatBoxChat");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		checkboxList = new ArrayList<JCheckBox>();
		Box buttonBox = new Box(BoxLayout.X_AXIS);
		Box chatBox = new Box(BoxLayout.Y_AXIS);
		
		JButton start = new JButton("Start");
		start.addActionListener(new MyStartListener());
		buttonBox.add(start);
		
		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopListener());
		buttonBox.add(stop);
		
		JButton upTempo = new JButton("upTempo");
		upTempo.addActionListener(new MyUpTempoListener());
		buttonBox.add(upTempo);
		
		JButton downTempo = new JButton("downTempo");
		downTempo.addActionListener(new MyDownTempoListener());
		buttonBox.add(downTempo);
		
		JButton saveButton = new JButton("Save track");
		saveButton.addActionListener(new MySaveListener());
		buttonBox.add(saveButton);
		
		JButton loadButton = new JButton("Load track");
		loadButton.addActionListener(new MyLoadListener());
		buttonBox.add(loadButton);
		
		JButton cleanButton = new JButton("Clean");
		cleanButton.addActionListener(new MyCleanListener());
		buttonBox.add(cleanButton);
		
		JButton sendButton = new JButton("send");
		sendButton.addActionListener(new MySendListener());
		buttonBox.add(sendButton);
		
//		JPanel chatPanel = new JPanel(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
//		chatPanel.setBorder(new TitledBorder("Столбец"));
		JLabel label = new JLabel("Мини чат:", JLabel.RIGHT);
		chatBox.add(label);
		userMessage = new JTextField();
		chatBox.add(userMessage);
		incomingList = new JList();
		incomingList.addListSelectionListener(new MyListSelectionListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane theList = new JScrollPane(incomingList);
		chatBox.add(theList);
		incomingList.setListData(listVector); // нет начальных данных
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for(int i = 0; i < 16; ++i) {
			nameBox.add(new Label(instrumentNames[i]));
		}
		
		
		theFrame.getContentPane().add(background);
		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(0);
		grid.setHgap(1);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);
		
		for (int i = 0; i < 256; ++i) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}
		
		Box playBox = new Box(BoxLayout.X_AXIS);
		playBox.add(nameBox);
		playBox.add(mainPanel);
		
		background.add(BorderLayout.NORTH, buttonBox);
		background.add(BorderLayout.WEST, playBox);
		background.add(BorderLayout.EAST, chatBox);
		
		theFrame.setBounds(100,100,300,300);
		theFrame.pack();
		theFrame.setVisible(true);
	}
	
	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		}catch (Exception ex) {ex.printStackTrace();}
	}
	
	public void buildTrackAndStart() {
		ArrayList<Integer> trackList = null;
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for (int i = 0; i < 16; ++i) {
			
			trackList = new ArrayList<Integer>();
			
			for (int j = 0; j < 16; ++j) {
				JCheckBox jc = (JCheckBox) checkboxList.get(j + (16 * i));
				if (jc.isSelected()) {
					int key = instruments[i];
					trackList.add(new Integer(key));
				}else {
					trackList.add(null);
				}
			}
			
			makeTracks(trackList);
		}
		
		track.add(makeEvent(192, 9, 1, 0, 15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		}catch(Exception e) {e.printStackTrace();}
	}
	
	public class MyStartListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			buildTrackAndStart();
		}
	}
	
	public class MyStopListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			sequencer.stop();
		}
	}
	
	public class MyUpTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor * 1.03));
		}
	}
	
	public class MyDownTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor * 0.97));
		}
	}
	
	public class MySaveListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			boolean[] checkboxState = new boolean[256];
			for(int i = 0; i < 256; ++i) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(check.isSelected()) {
					checkboxState[i] = true;
				}
			}
			
			try {
				FileOutputStream fileStream = new FileOutputStream(new File("Checkbox.ser"));
				ObjectOutputStream os = new ObjectOutputStream(fileStream);
				os.writeObject(checkboxState);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public class MyLoadListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			boolean[] checkboxState = null;
			try {
				FileInputStream fileIn = new FileInputStream(new File("Checkbox.ser"));
				ObjectInputStream is = new ObjectInputStream(fileIn);
				checkboxState = (boolean[]) is.readObject();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			for(int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(checkboxState[i]) {
					check.setSelected(true);
				} else {
					check.setSelected(false);
				}
			}
			
			sequencer.stop();
			buildTrackAndStart();
		}
	}
	
	class MyCleanListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			sequencer.stop();
			for(int i = 0; i < 256; ++i) {
				checkboxList.get(i).setSelected(false);	
			}	
		}
	}
	
	public class MySendListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			// создаем массив, в котором будут хранится только состояния флажков
			boolean[] checkboxState = new boolean[256];
			for (int i = 0; i < 256; ++i) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if (check.isSelected()) {
					checkboxState[i] = true;
				}
			}
			String messageToSend = null;
			try {
				out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
				out.writeObject(checkboxState);
			} catch (Exception ex) {
				System.out.println("Sorry dude. Couldn't send it to the server.");
			}
			userMessage.setText("");
		}
	}
	
	public class MyListSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent le) {
			if (!le.getValueIsAdjusting()) {
				String selected = (String) incomingList.getSelectedValue();
				if(selected != null) {
					// переходим к отображению и изменяем последовательность
					boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
					changeSequence(selectedState);
					sequencer.stop();
					buildTrackAndStart();
				}
			}
		}
	}
	
	public class RemoteReader implements Runnable {
		boolean[] checkboxState = null;
		String nameToShow = null;
		Object obj = null;
		public void run() {
			try {
				while((obj=in.readObject()) != null) {
					System.out.println("got an object from server");
					System.out.println(obj.getClass());
					String nameToShow = (String) obj;
					checkboxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, checkboxState);
					listVector.add(nameToShow);
					incomingList.setListData(listVector);
				}
			} catch (Exception ex) {ex.printStackTrace();}
		}
	}
	
	public class MyPlayMineListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			if (mySequence != null) {
				sequence = mySequence;
			}
		}
	}
	
	public void changeSequence(boolean[] checkboxState) {
		for (int i = 0; i < 256; ++i) {
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if (checkboxState[i]) {
				check.setSelected(true);
			} else {
				check.setSelected(false);
			}
		}
	}
	
	public void makeTracks(ArrayList list) {
		Iterator it = list.iterator();
		for(int i = 0; i < 16; ++i) {
			Integer num = (Integer) it.next();
			if (num != null) {
				int numKey = num.intValue();
				track.add(makeEvent(144, 9, numKey, 100, i));
				track.add(makeEvent(128, 9, numKey, 100, i+1));
			}
		}
	}
	
	public static MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, chan, one, two);
			event = new MidiEvent(a, tick);
		}catch(Exception e) {e.printStackTrace();}
			return event;
	}
}