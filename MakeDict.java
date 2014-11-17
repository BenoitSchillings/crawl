import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;


public class MakeDict {
	static int	counter = 0;
	static int  known = 0;
	static ObjectInputStream input;
	static ArrayList<String> dictionary = new ArrayList<String>();
	static ArrayList<Float> frequency = null; 

	static	Links[] map;
	static String path = "/media/benoit/09d1f277-6968-4ef1-9018-453bdfde4ce2/";

	public static int didx(String s)
	{
		int result = Collections.binarySearch(dictionary, s);
		if (result < 0)
			result = -1;

		return result;
	}

	public static void main(String[] args) throws Exception {
		// check for archive dictionary.
		if (new File(path + "dictionary.ser").isFile()) {
			FileInputStream fin = new FileInputStream(path + "dictionary.ser");
			ObjectInputStream ios = new ObjectInputStream(fin); 
			dictionary = (ArrayList<String>) ios.readObject();
			frequency = (ArrayList<Float>) ios.readObject();
			fin.close();
			ios.close();
		}
		else {
			// if not found, rebuild from scratch
			buildDict();
		}
		
			
		
		FileInputStream fileIn = new FileInputStream(path + "link_database");
		input = new ObjectInputStream(fileIn);

		int cnt = 0;
		
		//  do we already have the map computed?
		
		if (false && new File(path + "map.bin").isFile()) {

			FileInputStream ifl = new FileInputStream(path + "map.bin");
			DataInputStream in = new DataInputStream(ifl);

			System.out.println("load map");
			map = new Links[dictionary.size()];

			for (int idx = 0; idx < dictionary.size(); idx++) {
				map[idx] =   new Links();
				map[idx].readFromStream(in);
			}
			ifl.close();
			in.close();
		}
		else {
			buildMap();
		}
	}




	private static void CalcPopularity() throws IOException {
		frequency = new ArrayList<Float>();

		for (int i = 0; i < dictionary.size(); i++) {
			frequency.add(0.0f);
		}
		
		BufferedReader br = new BufferedReader(new FileReader(path + "freq.txt"));
	    
		String	line;
		do {
			line = br.readLine();
			if (line != null) {
				int start = line.indexOf(":en");
				int space1 = line.indexOf(" ", start + 4);
				int	space2 = line.indexOf(" ", space1 + 1);
				
				if (start >= 0 && space1 >= 0 && space2 >= 0) {
					String	name  = line.substring(start + 4, space1);
					float	count;
					
					count = Float.parseFloat(line.substring(space1 + 1, space2));
					
					int idx = didx(name);
					if (idx >= 0) {
						frequency.set(idx, frequency.get(idx) + count);
					}
				}
			
			}
			
		} while(line != null);

		br.close();
	}

	private static void buildMap() throws IOException {
		FileInputStream fileIn;

		fileIn = new FileInputStream(path + "link_database");
		input = new ObjectInputStream(fileIn);

		map = new Links[dictionary.size()];
		int	cnt = 0;
		
		while(Domap()) {
			cnt++;			
			if (cnt % 10000 == 0) {
				System.out.print(":");	
			}
		}
		FileOutputStream of = new FileOutputStream(path + "map.bin");
		DataOutputStream out = new DataOutputStream(of);
		out.writeInt(dictionary.size());
		
		for (int idx = 0; idx < dictionary.size(); idx++) {
			if (map[idx] != null) {
				map[idx].writeToStream(out);
			}
			else {
				out.writeInt(0);
			}
		}
		out.close();
	}

	private static void buildDict() throws IOException {
		FileInputStream fileIn;
		fileIn = new FileInputStream(path + "link_database");
		input = new ObjectInputStream(fileIn);

		int	cnt = 0;
		while(get()) {
			cnt++;			
			if (cnt % 10000 == 0) {
				System.out.print(".");

			}
		}

		Collections.sort(dictionary);
		
		CalcPopularity();
		
		int i = dictionary.size();
		
		
		//remove keywords without any read from the frequency list
		ArrayList<String> dict1 = null;
		ArrayList<Float> freq1 = null; 

		freq1 = (ArrayList<Float>) frequency.clone();
		dict1 = (ArrayList<String>) dictionary.clone();
		System.out.println("counta " + dictionary.size() + " " + frequency.size());

		frequency.clear();
		dictionary.clear();
		
		i = 0;
		
		while (i < dict1.size()) {
			// remove entries which have a frequency of zero
			if (freq1.get(i) > 2) {
				frequency.add(freq1.get(i));
				dictionary.add(dict1.get(i));
				//System.out.println(dict1.get(i) + " " + freq1.get(i));
			}
			i++;
			if ((i%1000) == 0)
				System.out.print("&");
		}

		FileOutputStream fout = new FileOutputStream(path + "dictionary.ser");
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(dict1);
		oos.writeObject(freq1);
		System.out.println("count " + dictionary.size() + " " + dict1.size());
		oos.close();
		fileIn.close();
		input.close();
	}

	private static boolean get() {
		String	name;
		List<String> list_of_links = new ArrayList<String>();

		try {
			name = (String) input.readObject();
			list_of_links = (List<String>) input.readObject();
			dictionary.add(name);
		} catch (ClassNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

		return true;
	}


	private static boolean Domap() {
		String	name;
		List<String> list_of_links = new ArrayList<String>();

		try {
			name = (String) input.readObject();
			list_of_links = (List<String>) input.readObject();
		} catch (ClassNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

		int	index;

		index = didx(name);

		if (index >= 0) {
			map[index] = new Links();
			map[index].setName(name);

			for (String s : list_of_links) {
				int	lnk = didx(s);
				if (lnk >= 0) {
					map[index].addLink(lnk);
				}
			}
		}
		return true;
	}

}
