import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.zip.GZIPInputStream;


public class Trend {
	
	static String workval(int v)
	{
		return String.format("%02d", v);
	}
	
	@SuppressWarnings("deprecation")
	static
	String timeToName(Calendar time)
	{
		String name = "http://dumps.wikimedia.org/other/pagecounts-raw/";
		
		name = name + time.get(Calendar.YEAR) + "/";
		name = name + time.get(Calendar.YEAR) + "-" + workval(time.get(Calendar.MONTH)) + "/";
		name = name + "pagecounts-" +
					   time.get(Calendar.YEAR) + "" + workval(time.get(Calendar.MONTH)) + "" + 
					   workval(time.get(Calendar.DATE));
		name = name + "-" + workval(time.get(Calendar.HOUR)) + "0000.gz";
		
		
		return name;
	}
	
	static
	String timeToNamePart(Calendar time)
	{
		String name = "";
		
		name = name + time.get(Calendar.YEAR) + "-" + workval(time.get(Calendar.MONTH)) + "-";
		name = name + "pagecounts-" +
					   time.get(Calendar.YEAR) + "" + workval(time.get(Calendar.MONTH)) + "" + 
					   workval(time.get(Calendar.DATE));
		name = name + "-" + workval(time.get(Calendar.HOUR)) + "0000.txt";
		
		System.out.println(name);
		return name;
	}

	
	public static void main(String[] args) throws Exception {
		Calendar cal = new GregorianCalendar(2015, 03, 01);
		cal.add(Calendar.HOUR, 3);
		

		for (int i = 0; i < 210; i++) {
			System.out.println(timeToName(cal));
			URL website = new URL(timeToName(cal));
			ReadableByteChannel rbc = Channels.newChannel(new GZIPInputStream(website.openStream()));
			FileOutputStream fos = new FileOutputStream("/media/benoit/09d1f277-6968-4ef1-9018-453bdfde4ce2/freq/" + timeToNamePart(cal));
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			cal.add(Calendar.HOUR, 1);
		}
		
	}


	private static Calendar GregorianCalendar(int i, int j, int k) {
		// TODO Auto-generated method stub
		return null;
	}
}
