import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.zip.InflaterInputStream;
import java.io.UnsupportedEncodingException;
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


public class BigH {
	static Map<String, Integer> currentDict = new HashMap<String, Integer>();
	static Map<String, Integer> mRefMap = null;
	static int	counter = 0;
	static int  known = 0;
	private static FileWriter tfstream;
	private static BufferedWriter links;
	private static int gMaxLevel;

	static String name_to_dir(String name)
	{
		if (name.startsWith("Cat"))
			return name.substring(0, 3) + name.hashCode() % 256;
		else
			return "cache" + name.hashCode() % 2048;
	}
	static String get_url(String subject, boolean reload) throws Exception
	{

		String content;
		File file;
		String path_to_cache = "/media/benoit/09d1f277-6968-4ef1-9018-453bdfde4ce2/cache/";

		if (reload == false)
		try {
			String SubDir =  path_to_cache + name_to_dir(subject);
			file = new File( SubDir  + "/" + subject + ".zip");
			ZipFile reader = new ZipFile(file);
			InputStream s = reader.getInputStream(reader.entries().nextElement());
			byte[] chars = new byte[s.available()];
			if (s.available() == 0)
				return "";
			s.read(chars, 0, s.available());
			content = new String(chars);
			if (!content.startsWith("<!DOCTYPE html>")) {
				System.out.println("bad file");
				throw(new Exception());
			}
			//System.out.println("Found");
			s.close();
			reader.close();
			return content;
		} catch (Exception e) {
			System.out.println("fnf " + subject);
		}
		

		StringBuilder buf = new StringBuilder();
		try {
			URL url = new URL("http://en.wikipedia.org/wiki/" + subject);

			URLConnection con = url.openConnection();
			if (con == null)
				return null;
			//con.setRequestProperty("Accept-Encoding", "gzip, deflate");

			Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
			Matcher m = p.matcher(con.getContentType());
			String charset = m.matches() ? m.group(1) : "ISO-8859-1";
			
			
			String encoding = con.getContentEncoding();
			//System.out.println(encoding);
			InputStream inStr = null;

			// create the appropriate stream wrapper based on
			// the encoding type
			if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
			    inStr = new GZIPInputStream(con.getInputStream());
			} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
			    inStr = new InflaterInputStream(con.getInputStream(),
			      new Inflater(true));
			} else {
			    inStr = con.getInputStream();
			}
			
			
			Reader r = new InputStreamReader(con.getInputStream(), charset);
			while (true) {
				int ch = r.read();
				if (ch < 0)
					break;
				buf.append((char) ch);
			}
		} catch (FileNotFoundException e) {
			System.out.println("no url");
		}

		String str = buf.toString();
		//System.out.println("write");
		try {
			String SubDir = path_to_cache + name_to_dir(subject);
			System.out.println(SubDir  + "/" + subject + ".zip");
			new File(SubDir).mkdir();
			SubDir = path_to_cache + name_to_dir(subject);
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(SubDir  + "/" + subject + ".zip"));

			out.putNextEntry(new ZipEntry("subject"));
			out.write(str.getBytes(Charset.forName("UTF-8")));
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("bad filename");
		}

		return str;
	}

	public static void main(String[] args) throws Exception {
		//java Crawl cache : Rescan the whole cache

		gMaxLevel = 2;
		if (args[0].equals("cache")) {
			while(gMaxLevel < 21) {
				gMaxLevel+=1;
				scanner("Category:Contents");
				//scanner("Portal:Contents/Categories");
			}
		}
	}
	


	private static void scanner(String name) throws Exception {

		scan("", name, 0);
	}


	private static void scan(String path, String string, int level) throws Exception {
		String markup;
		known = 0;
		
		if (level > gMaxLevel)
			return;
		try {
			markup = get_url(string, false);
		} catch (UnsupportedEncodingException e) {
			return;
		} catch (IOException e) {
			System.out.println("failed get_url " + e);
			return;
		}
		if (markup == null) {
			System.out.println("empty load for " + string);	
			return;
		}


		List<String> list_of_strings = parse_links(markup);
		int len = list_of_strings.size();
		/*
		if (len == 0) {
			markup = get_url(string, true);
			list_of_strings = parse_links(markup);
			System.out.println("len1 is " + list_of_strings.size());
			len = list_of_strings.size();

		}
		*/
		if (len < 1000) {
			for (int i = 0; i < len; i++) {
				String val = list_of_strings.get(i);
				int current_level = Integer.MAX_VALUE;
				if (currentDict.get(val) != null) {
					current_level = currentDict.get(val);
				}
				if (current_level > level) {
					currentDict.put(val, level);
				}
				else {
					//System.out.println("[" + gMaxLevel + "]" + path + "|" + list_of_strings.get(i));
					scan(path + "|" + catName(val), val, level + 1);
				}
			}
		}

	}



	private static String catName(String val) {
		// TODO Auto-generated method stub
		//return val.substring(9);
		return val;
	}

	private static List<String> parse_links(String markup) {
		List<String> list_of_links = new ArrayList<String>();
		String findStr = "href=\"/wiki/";


		int find_length = findStr.length();
		int lastIndex = 0;
		int length = markup.length();

		String reducers[] = {"mw-normal-catlinks", "id=\"References\"", "id=\"See_also\""};
		int max_length = markup.indexOf("mw-normal-catlinks");

		if (max_length > 0) {
			length = max_length;
		}

		for (String s : reducers) {
			max_length = markup.indexOf(s);
			if (max_length > 0) {
				if (max_length < length) {
					length = max_length;
				}
			}
		}
		//System.out.println(markup);
		lastIndex = markup.indexOf("mw-subcategories");
		//if (lastIndex < 0) lastIndex = 0;
		//else
		//System.out.println("Crop1");
		while(lastIndex != -1) {

			lastIndex = markup.indexOf(findStr, lastIndex);
			if( lastIndex != -1) {
				if (markup.charAt(lastIndex - 1) == ' ' || markup.charAt(lastIndex - 1) == '>') {

					char c;
					int cnt = 0;
					do {
						int	idx = lastIndex + find_length + cnt;

						if (idx >= length)
							break;
						if (cnt > 40)
							break;
						c = markup.charAt(idx);
						if (c != '\"') {
							cnt++;
						}
					}
					while (c != '\"');

					String s;
					if (find_length > 0) {
						s = markup.substring(lastIndex + find_length, lastIndex + find_length + cnt);
						if (check_link(s)) {
							if (!list_of_links.contains(s))
								list_of_links.add(s);
						}
					}
				}
				lastIndex += findStr.length();
			}
		}


		return list_of_links;
	}

	private static boolean check_link(String s) {

		if (s.length() < 3)
			return false;
		if (s.length() > 40)
			return false;
		if (noisyWord(s))
			return false;


		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c == '%')
				return false;
			if (c == '#')
				return false;
			if (c == '/')
				return false;
			if (c == ',')
				return false;
			if (c == '(')
				return false;
			if (c == ')')
				return false;
		}

		char c = s.charAt(0);
		if (c >= 'A' && c <= 'Z')
			return true;
		if (c >= '0' && c <= '9')
			return true;

		return false;

	}

	private static boolean noisyWord(String s) {
		if (s.contains("Wikipedia_administration"))
			return true;
		if (s.contains("Wikipedia"))
			return true;
		if (s.endsWith("_stubs")) 
			return true;

		if (s.contains("Category:"))
			return false;
		//if (s.contains("Portal:"))
			//return false;
		if (s.contains(":")) {
			//System.out.println(s);
			return true;
		}
		//System.out.println(s);
		//if (s.startsWith("Lists_of_"))
		//return false;
		//if (s.startsWith("List_of_"))
		//return false;
		return true;
	}
}
