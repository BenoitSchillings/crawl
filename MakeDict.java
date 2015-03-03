import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
	private static final Short MIN_FREQ = 45;	//anything seen less than 35 times over the last week is likely not that interesting
	private static final int MAX_LINK = 1500;	//anything with more than 200 links is likely a table or category
	private static final Integer MAX_REF = 15000;	//anything reference by more than 4000 pages is likely noise

	static int	counter = 0;
	static int  known = 0;
	static ObjectInputStream input;
	static ArrayList<String> dictionary = new ArrayList<String>();
	static ArrayList<Short> frequency = null; 
	static ArrayList<Integer> refcount = null;

	static	Links[] map;
	static String path = "/media/benoit/09d1f277-6968-4ef1-9018-453bdfde4ce2/";

	public static int didx(String s)
	{
		int result = Collections.binarySearch(dictionary, s.toUpperCase());
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
			frequency = (ArrayList<Short>) ios.readObject();
			fin.close();
			ios.close();
		}
		else {
			// if not found, rebuild from scratch
			buildDict();
		}



		FileInputStream fileIn = new FileInputStream(path + "link_database");
		input = new ObjectInputStream(new BufferedInputStream(fileIn, 256*256*256));

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


	private static void CalcRefcount()
	{
		refcount = new ArrayList<Integer>();

		for (int i = 0; i < dictionary.size(); i++) {
			refcount.add(0);
		}

		for (int idx = 0; idx < dictionary.size(); idx++) {
			if (map[idx].list != null) {
				ArrayList<Integer> temp = (ArrayList<Integer>)map[idx].list;
				for (int j = 0; j < temp.size(); j++) {
					int sub = temp.get(j);
					refcount.set(sub, refcount.get(sub) + 1);
				}
			}
		}
	}


	private static void CalcPopularity() throws IOException {
		frequency = new ArrayList<Short>();

		for (int i = 0; i < dictionary.size(); i++) {
			frequency.add((short) 0);
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
					int	count;

					count = Integer.parseInt(line.substring(space1 + 1, space2));

					int idx = didx(name);
					if (idx >= 0) {
						count = count + frequency.get(idx);
						if (count > 32000)
							count = 32000;
						frequency.set(idx, (short)count);
					}
				}

			}

		} while(line != null);

		br.close();
	}


	// rebuild the map for every entry that exists in the dictionary.

	private static void buildMap() throws IOException {
		FileInputStream fileIn;

		fileIn = new FileInputStream(path + "link_database");
		input = new ObjectInputStream(new BufferedInputStream(fileIn, 256*256*256));

		map = new Links[dictionary.size()];
		int	cnt = 0;

		while(Domap()) {
			cnt++;			
			if (cnt % 10000 == 0) {
				System.out.println(cnt);	
			}
		}
		CalcRefcount();

		FileOutputStream of = new FileOutputStream(path + "map.bin");
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(of, 256*256*256));
		out.writeInt(dictionary.size());

		// cull non reciprocal links and items refered to with more than MAX_REF incoming links


		for (int idx = 0; idx < dictionary.size(); idx++) {
			ArrayList<Integer> temp = (ArrayList<Integer>)map[idx].list;
			map[idx].list = new ArrayList<Integer>();

			for (int j = 0; j < temp.size(); j++) {
				int sub = temp.get(j);
				if (map[sub] != null) {
					if (map[sub].list.contains(idx)) {
						if (refcount.get(sub) < MAX_REF) {	
							map[idx].addLink(sub);
						}
					}
				}
			}
			if (map[idx].list.size() == 0) {
				map[idx] = null;
			}
			else {
				sortMap(idx);
				while (map[idx].list.size() > 22) {
					map[idx].list.remove(22);
				}
			}
		}


		for (int idx = 0; idx < dictionary.size(); idx++) {
			if (map[idx] != null) {
				map[idx].writeToStream(out);
			}
			else {
				out.writeInt(0);
			}
		}
		out.close();
		writeMapToText(map);
	}


	private static void writeMapToText(Links[] a_map) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(path + "map.txt"));
		writer.write(a_map.length + "\n");

		for (int i = 0; i < a_map.length; i++) {
			writer.write(a_map[i].count() + "\t");
			for (int j = 0; j < a_map[i].count(); j++) {
				writer.write("\t" + a_map[i].list.get(j));
			}
			writer.write("\n");
		}
		writer.close();
	}


	private static void sortMap(int new_match) {

		Comparator<Integer> PopComparator = new Comparator<Integer>() {

			@Override

			public int compare(Integer v1, Integer v2) {

				return frequency.get(v2) - frequency.get(v1);

			}

		};

		Collections.sort(map[new_match].list, PopComparator);

	}

	private static void buildDict() throws IOException {
		FileInputStream fileIn;
		fileIn = new FileInputStream(path + "link_database");
		input = new ObjectInputStream(new BufferedInputStream(fileIn, 256*256*256));


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
		ArrayList<Short> freq1 = null; 

		freq1 = (ArrayList<Short>) frequency.clone();
		dict1 = (ArrayList<String>) dictionary.clone();
		System.out.println("counta " + dictionary.size() + " " + frequency.size());

		frequency.clear();
		dictionary.clear();

		i = 0;

		while (i < dict1.size()) {
			// remove entries which have a frequency of zero or entries with more than 500 links
			if (freq1.get(i) > MIN_FREQ) {
				frequency.add(freq1.get(i));
				dictionary.add(dict1.get(i));
				//System.out.println(dict1.get(i) + " " + freq1.get(i));
			}
			else {
				//System.out.println("remove because of frequency " + dict1.get(i));
			}

			i++;
			if ((i%10000) == 0)
				System.out.print("&");
		}

		FileOutputStream fout = new FileOutputStream(path + "dictionary.ser");
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(dictionary);
		oos.writeObject(frequency);
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
			dictionary.add(name.toUpperCase());
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
			if (list_of_links.size() > MAX_LINK) {
				map[index] = new Links();
				map[index].setName("");
				return true;
			}

			map[index] = new Links();
			map[index].setName(name);


			for (String s : list_of_links) {
				int	lnk = didx(s);
				if (lnk >= 0) {
					if (frequency.get(lnk) > MIN_FREQ) {
						map[index].addLink(lnk);
					}
				}
			}
		}

		return true;
	}

}
