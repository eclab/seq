/* 
   Copyright 2026 by Sean Luke and George Mason University
   Licensed under Apache 2.0
*/

package seq.motif.generator.alg.dice;

import seq.engine.*;
import seq.motif.blank.*;
import seq.motif.generator.*;
import seq.util.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import java.io.*;
import seq.motif.generator.gui.*;
import seq.motif.generator.alg.dice.gui.*;

public class Dice extends Algorithm
    {
    private static final long serialVersionUID = 1;
    
    public static final int RATE_WHOLE_NOTES = Seq.PPQ * 4;
    public static final int RATE_HALF_NOTES = Seq.PPQ * 2;
    public static final int RATE_QUARTER_NOTES = Seq.PPQ;
    public static final int RATE_EIGHTH_NOTES = Seq.PPQ / 2;
    public static final int RATE_TRIPLET_NOTES = Seq.PPQ / 3;
    public static final int RATE_SIXTEENTH_NOTES = Seq.PPQ / 4;
    public static final int RATE_TRIPLET_SIXTEENTH_NOTES = Seq.PPQ / 6;
    public static final int RATE_THIRTY_SECOND_NOTES = Seq.PPQ / 8;
        
    public static final int[] RATES = { RATE_WHOLE_NOTES, RATE_HALF_NOTES, RATE_QUARTER_NOTES, RATE_EIGHTH_NOTES, RATE_TRIPLET_NOTES, RATE_SIXTEENTH_NOTES, RATE_TRIPLET_SIXTEENTH_NOTES, RATE_THIRTY_SECOND_NOTES };
    public static final String[] RATE_NAMES = { "Whole Note", "Half Note", "Quarter Note", "Eighth Note", "Triplet", "Sixteenth Note", "Triplet Sixteenth Note", "Thirty-Second Note" };
		
    public int getRateIndex()
        { 
        for(int i = 0; i < RATES.length; i++) 
            {
            if (RATES[i] == rate) return i;
            }
        return 0;
        }
        
    public static final int MAX_TRANSPOSE = 48;
    public static final int LOW_NOTE = 60 - 24;

	double gate = 0.5;
    int transpose = 24;
    int velocity = 64;
    int rate = RATE_EIGHTH_NOTES;

	int[/*songs*/][/*measures*/][/*dice*/] indices;
	int[/*melodies*/][/*chords*/][/*notes*/] treble;
	int[/*melodies*/][/*chords*/][/*notes*/] bass;

	public int[][][] getIndices() { return indices; }
	public int[][][] getTreble() { return treble; }
	public int[][][] getBass() { return bass; }

    public static final int[] NOTE_VALS = { 9, 11, 0, 2, 4, 5, 7 };  // A, B, C, D, E, F, G
    
    //public static final String[] KEYS = { "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B" };

	int getNote(String val)
		{
		int n = (int)(Character.toLowerCase(val.charAt(0)) - 'a');
		boolean sharp = val.charAt(1) == ('#');
		boolean flat = val.charAt(1) == ('b');
		n = NOTE_VALS[n];
		if (sharp) n++;
		else if (flat) n--;
		if (n < 0) n = 11;		// cb -> b
		if (n >= 12) n = 0;		// b# -> c, should we wrap around in this case?
		
		try
			{
			int oct = Integer.parseInt(val.substring(sharp || flat ? 2 : 1));
			//int pitch = oct * 12 + n;
			//System.err.println(val + " -> " + KEYS[pitch % 12] + (pitch / 12));
			return oct * 12 + n;
			}
		catch (NumberFormatException ex)
			{
			System.err.println("Dice.getNote NumberFormatException in String #" + val + "#\n" + ex); 
			return -1;
			}
		}
		
	void loadJSON(JSONObject data)
		{
			// Load indices arrays
			
			JSONArray indicesData = data.getJSONArray("indices");
			indices = new int[indicesData.length()][][];
			for(int i = 0; i < indices.length; i++)
				{
				JSONArray indicesData2 = indicesData.getJSONArray(i);
				indices[i] = new int[indicesData2.length()][];
				for(int j = 0; j < indices[i].length; j++)
					{
					JSONArray indicesData3 = indicesData2.getJSONArray(j);
					indices[i][j] = new int[indicesData3.length()];
					for(int k = 0; k < indices[i][j].length; k++)
						{
						indices[i][j][k] = indicesData3.getInt(k);
						}
					}
				}
				
			// Load fragments
			JSONArray fragmentsData = data.getJSONArray("fragments");
			treble = new int[fragmentsData.length()][][];
			bass = new int[treble.length][][];
			for(int i = 0; i < treble.length; i++)			// fragments 0...i...176
				{
				JSONObject trebleAndBass = fragmentsData.getJSONObject(i);
				JSONArray trebleData = trebleAndBass.getJSONArray("treble");
				JSONArray bassData = trebleAndBass.getJSONArray("bass");
				treble[i] = new int[trebleData.length()][];
				for(int j = 0; j < treble[i].length; j++)		// treble chord run
					{
					JSONArray trebleChord = trebleData.getJSONArray(j);
					treble[i][j] = new int[trebleChord.length()];
					if (treble[i][j].length == 1) // rest
						{
						treble[i][j][0] = trebleChord.getInt(0);
						}
					else
						{
						JSONArray notes = trebleChord.optJSONArray(0);
						if (notes != null) 	// it's a chord
							{
							treble[i][j] = new int[notes.length() + 1];
							for(int k = 0; k < treble[i][j].length - 1; k++)
								{
								treble[i][j][k] = getNote(notes.getString(k));
								}
							}
						else
							{
							treble[i][j] = new int[2];
							treble[i][j][0] = getNote(trebleChord.getString(0));
							}
						treble[i][j][treble[i][j].length - 1] = trebleChord.getInt(1);
						}
					}

				bass[i] = new int[bassData.length()][];
				for(int j = 0; j < bass[i].length; j++)		//	bass chord run 
					{
					JSONArray bassChord = bassData.getJSONArray(j);
					bass[i][j] = new int[bassChord.length()];
					if (bass[i][j].length == 1) // rest
						{
						bass[i][j][0] = bassChord.getInt(0);
						}
					else
						{
						JSONArray notes = bassChord.optJSONArray(0);
						if (notes != null) 	// it's a chord
							{
							bass[i][j] = new int[notes.length() + 1];
							for(int k = 0; k < bass[i][j].length - 1; k++)
								{
								bass[i][j][k] = getNote(notes.getString(k));
								}
							}
						else
							{
							bass[i][j] = new int[2];
							bass[i][j][0] = getNote(bassChord.getString(0));
							}
						bass[i][j][bass[i][j].length - 1] = bassChord.getInt(1);
						}
					}
				}
		}
		
	int[][][] copyArray(int[][][] array)
		{
		int[][][] copy = new int[array.length][][];
		for(int i = 0; i < copy.length; i++)
			{
			copy[i] = Motif.copy(array[i]);
			}
		return copy;
		}
		
    public Dice copy()
        {
        Dice lm = (Dice)(super.copy());
        lm.indices = copyArray(indices);
        lm.treble = copyArray(treble);
        lm.bass = copyArray(bass);
        return lm;
        }
    
	public static final String DICE_FILE = "dice.json";
		
    public Dice(Generator generator)
        {
        super(generator);
        loadJSON(new JSONObject(new JSONTokener(getClass().getResourceAsStream(DICE_FILE))));
        }
        
    public Dice(Generator generator, JSONObject obj) throws JSONException
        {
        super(generator, obj);
        loadJSON(new JSONObject(new JSONTokener(getClass().getResourceAsStream(DICE_FILE))));

        transpose = obj.optInt("trans", 0);
        velocity = obj.optInt("vel", 64);
        rate = obj.optInt("rate", RATE_EIGHTH_NOTES);
        gate = obj.optDouble("gate", 0.5);
        }
        
    public void save(JSONObject obj) throws JSONException
        {
        super.save(obj);
        obj.put("trans", transpose);
        obj.put("vel", velocity);
        obj.put("rate", rate);
        obj.put("gate", gate);
        }

    public AlgorithmNode buildNode(Seq seq, GeneratorClip clip)
        {
        return new DiceNode(seq, generator, clip, this);
        }
                
    public AlgorithmUI buildUI(Seq seq, GeneratorUI ui)
        {
        return new DiceUI(seq, generator, ui, this);
        } 

    public int getTranspose() { return transpose; }
    public void setTranspose(int val) { transpose = val; }
    public int getVelocity() { return velocity; }
    public void setVelocity(int val) { velocity = val; }
    public int getRate() { return rate; }
    public void setRate(int val) { rate = val; }
    public double getGate() { return gate; }
    public void setGate(double val) { gate = val; }

    public String getHTMLDescription() 
        {
        return getIndexHTMLFileText();
        }
                
    }
        
        
        
        
        
        
