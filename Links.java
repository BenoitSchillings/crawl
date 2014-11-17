import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Links  {
	ArrayList<Integer> list = new ArrayList<Integer>();
	

	void	addLink(int link)
	{
		list.add(link);
	}

	int		count()
	{
		return list.size();
	}

	int		get(int idx)
	{
		int	result;
		
		result = list.get(idx);

		return result;
	}

	public void setName(String name) {
	}


	public void Trim() {
		//list.trimToSize();		
	}


	public void readFromStream(DataInputStream in) throws IOException {
		int i = in.readInt();
		
		
		list.clear();
		
		
		while(i > 0) {
			i--;
			list.add(in.readInt());
		}
	}
	
	
	public void writeToStream(DataOutputStream out) throws IOException {
		out.writeInt(count());
		
		int	i;
		
		i = count();
		while(i > 0) {
			i--;
			out.writeInt(get(i));
		}
	}
}
