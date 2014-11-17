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
import java.nio.ByteBuffer;
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


public class Digest {
	static int	counter = 0;
	static int  known = 0;
	static ObjectInputStream input;
	static ArrayList<String> dictionary = new ArrayList<String>();
	static ArrayList<Float> frequency = null; 

	static String path = "/media/benoit/09d1f277-6968-4ef1-9018-453bdfde4ce2/";

	static	Vecs[] map;

	public static int didx(String s)
	{
		int result = Collections.binarySearch(dictionary, s);
		if (result < 0)
			result = -1;

		return result;
	}

	public static void main(String[] args) throws Exception {
		if (new File(path + "dictionary.ser").isFile()) {
			FileInputStream fin = new FileInputStream(path + "dictionary.ser");
			ObjectInputStream ios = new ObjectInputStream(fin); 
			dictionary = (ArrayList<String>) ios.readObject();
			frequency = (ArrayList<Float>) ios.readObject();
			fin.close();
			ios.close();
		}

		{
			FileInputStream fin = new FileInputStream(path + "map.bin");
			DataInputStream in = new DataInputStream(fin);

			ByteBuffer work_buffer = ByteBuffer.allocate(1000000000);


			in.read(work_buffer.array(), 0, 700000000);


			int cnt = work_buffer.getInt();
			map = new Vecs[cnt];
			System.out.println(cnt);

			for (int idx = 0; idx < cnt; idx++) {
				map[idx] = new Vecs();
				map[idx].readFromStream(work_buffer);
				if (idx % 10000 == 0) System.out.print(".");
			}

			fin.close();
			in.close();


			for (int idx = 0; idx < cnt; idx++) {
				for (int j = 0; j < map[idx].count(); j++) {
					int ref = map[idx].array[j];
					map[ref].refCount++;
				}
			}

			ArrayList<String> dict1 = new ArrayList<String>();
			ArrayList<Float> freq1  = new ArrayList<Float>(); 
/*
			for (int idx = 0; idx < cnt; idx++) {
				if (map[idx].refCount > 5 && map[idx].refCount < 200 && map[idx].count() > 7 && map[idx].count() < 300) {
					System.out.println(dictionary.get(idx) + " " + map[idx].count() + " " + map[idx].refCount + " " + frequency.get(idx));
					dict1.add(dictionary.get(idx));
					freq1.add(frequency.get(idx));
				}
			}
*/
			for (int idx = 0; idx < cnt; idx++) {
				System.out.print(dictionary.get(idx) + " " + map[idx].count() + " " + map[idx].refCount + " " + frequency.get(idx) + " ");
					for (int j = 0; j < map[idx].count();j++) {
						int	subref;
						
						subref = map[idx].array[j];
						if (map[subref].contains(idx)) {
							System.out.print(dictionary.get(subref)  + "[" + map[subref].refCount + "]" + "[" + frequency.get(subref) + "]"+ ",");
						}
					}
					System.out.println(" ");
			}

/*			
			FileOutputStream fout = new FileOutputStream(path + "dictionary.ser");
			ObjectOutputStream iout = new ObjectOutputStream(fout); 
			iout.writeObject(dict1);
			iout.writeObject(freq1);
			fout.close();
			iout.close();
	*/		
		}

	}

}

