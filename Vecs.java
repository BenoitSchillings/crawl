import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Vecs  {
	int[]	array;
	int		refCount;
	
	Vecs()
	{
		refCount = 0;
		array = null;
	}

	int		count()
	{
		if (array == null)
			return 0;
		
		return array.length;
	}


	public void readFromStream(ByteBuffer work_buffer) throws IOException {
		int	cnt = work_buffer.getInt();
		

		if (cnt == 0) {
			array = null;
			return;
		}
		

		array = new int[cnt];
		for (int i = 0; i < cnt; i++) {
			array[i] = work_buffer.getInt();
		}
	}

	public boolean contains(int idx) {
		int	cnt = count();
		
		for (int i = 0; i < cnt; i++) {
			if (idx == array[i])
				return true;
		}
		return false;
	}
}
