package by.first.head;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class BeatBox {
	private JPanel mainPanel;
	private ArrayList<JCheckBox> checkBoxList;
	private Sequencer sequencer;
	private Sequence sequence;
	private Track track;
	private JFrame frame;

	private String[] instrumentNames = { "Bass Drum", "Closet Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
			"Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap",
			"Low-mid Tom", "High Agogo", "Open Hi Conga" };

	private int[] instruments = { 35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63 };

	public static void main(String[] args) {
		new BeatBox().buildGUI();
	}

	public void buildGUI() {
		frame = new JFrame("BeatBox");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		BorderLayout layot = new BorderLayout();
		JPanel background = new JPanel(layot);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		checkBoxList = new ArrayList<JCheckBox>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);

		JButton start = new JButton("Start");
		start.addActionListener(new StartListener());
		buttonBox.add(start);

		JButton stop = new JButton("Stop");
		stop.addActionListener(new StopListener());
		buttonBox.add(stop);

		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(new UpTempoListener());
		buttonBox.add(upTempo);

		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new DownTempoListener());
		buttonBox.add(downTempo);
		
		JButton serialize = new JButton("Serialize It");
		serialize.addActionListener(new SerializeListener());
		buttonBox.add(serialize);
		
		JButton restore = new JButton("Restore");
		restore.addActionListener(new RestoreListener());
		buttonBox.add(restore);

		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for (int i = 0; i < 16; i++) {
			nameBox.add(new Label(instrumentNames[i]));
		}

		background.add(buttonBox, BorderLayout.EAST);
		background.add(nameBox, BorderLayout.WEST);

		frame.getContentPane().add(background);

		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(mainPanel, BorderLayout.CENTER);

		for (int i = 0; i < 256; i++) {
			JCheckBox checkBox = new JCheckBox();
			checkBox.setSelected(false);
			checkBoxList.add(checkBox);
			mainPanel.add(checkBox);
		}

		setUpMidi();
		
		frame.setBounds(50, 50, 300, 300);
		frame.pack();
		frame.setVisible(true);

	}

	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void buildTrackAndStart() {
		int[] trackList = null;

		sequence.deleteTrack(track);
		track = sequence.createTrack();

		for (int i = 0; i < 16; i++) {
			trackList = new int[16];
			int key = instruments[i];

			for (int j = 0; j < 16; j++) {
				JCheckBox jc = (JCheckBox) checkBoxList.get(j + (16 * i));
				if (jc.isSelected()) {
					trackList[j] = key;
				} else {
					trackList[j] = 0;
				}
			}

			makeTrack(trackList);
			track.add(makeEvent(176, 1, 127, 0, 16));
		}

		track.add(makeEvent(192, 9, 1, 0, 15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class StartListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			buildTrackAndStart();
		}

	}

	public class StopListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			sequencer.stop();
		}

	}

	public class UpTempoListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 1.03));
		}

	}

	public class DownTempoListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * .97));
		}

	}

	public void makeTrack(int[] list) {
		for (int i = 0; i < 16; i++) {
			int key = list[i];
			
			if (key != 0) {
				track.add(makeEvent(144, 9, key, 100, i));
				track.add(makeEvent(128, 9, key, 100, i+1));
			}
		}
	}
	
	public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, chan, one, two);
			event = new MidiEvent(a, tick);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return event;
	}
	
	
	private class SerializeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean[] checkBoxState = new boolean[256];  // for the state of each checkBox
			
			// save the state of each checkBox
			for (int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkBoxList.get(i);
				if (check.isSelected()) {
					checkBoxState[i] = true;
				}
			}
			
			// serialize boolean array
			try {
				
				JFileChooser fileSave = new JFileChooser();
				fileSave.showSaveDialog(frame);  
				FileOutputStream fileStream = new FileOutputStream(fileSave.getSelectedFile());
				ObjectOutputStream os = new ObjectOutputStream(fileStream);
				os.writeObject(checkBoxState);
				os.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
		}

	}
	
	
	private class RestoreListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean[] checkBoxState = null;
			
			// read object (boolean array) from file 
			try {
				JFileChooser fileOpen = new JFileChooser();
				fileOpen.showOpenDialog(frame);
				FileInputStream fileIn = new FileInputStream(fileOpen.getSelectedFile());
				ObjectInputStream is = new ObjectInputStream(fileIn);
				checkBoxState = (boolean[]) is.readObject();
				is.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			// restore the state of each checkBox
			for (int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkBoxList.get(i);
				if (checkBoxState[i]) {
					check.setSelected(true);
				} else {
					check.setSelected(false);
				}
			}
			
			// stop the music and restore old state 
			sequencer.stop();
			buildTrackAndStart();
			
		}

	}
}
