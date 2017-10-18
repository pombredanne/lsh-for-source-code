import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class LocSenHash {
	public int noofdoc=0;
	public int stage=0;
	public int bucket=0;
	public ArrayList<String> tokenlist=new ArrayList<String>();
	public ArrayList<String> allfileidlist=new ArrayList<String>();
	public String filename=null;
	public ArrayList<ArrayList<Integer>> alltokenposlist = new ArrayList<ArrayList<Integer>>();
	public int[][] doctermmatr;


	public void setNoofDoc(int docno)
	{
		noofdoc=docno;
	}

	public int getNoofDoc()
	{
		return noofdoc;
	}

	public void setStages(int stages)
	{
		stage=stages;
	}

	public int getStages()
	{
		return stage;
	}


	public void setBucket(int buckets)
	{
		bucket=buckets;
	}

	public int getBucket()
	{
		return bucket;
	}

	public void setAllFileIdList(ArrayList<String> Allfileidlist)
	{
		allfileidlist = Allfileidlist;
	}

	public ArrayList<String> getAllFileIdList()
	{
		return allfileidlist;
	}

	public void setAllTokenPosList(ArrayList<ArrayList<Integer>> Tokenlist)
	{
		alltokenposlist = Tokenlist;
	}

	public ArrayList<ArrayList<Integer>> getAllTokenPosList()
	{
		return alltokenposlist;
	}

	public int  getsinglevalue(int i,int j)
	{
		return doctermmatr[i][j];
	}

	public void initDocTermMatr(int row,int col)
	{
		doctermmatr = new int[row][col];

		for(int i=0;i<row;i++)
		{
			for(int j=0;j<col;j++)
			{
				doctermmatr[i][j]=0;
			}
		}
	}

	public void setDocTermMatr(int [][] DocTermmatr)
	{
		doctermmatr = DocTermmatr;

	}

	public int[][] getDocTermMatr()
	{
		return doctermmatr;
	}

	public void setTokenList(ArrayList<String> Tokenlist)
	{

		tokenlist = Tokenlist;
	}

	public ArrayList<String> getTokenList()
	{
		return tokenlist;
	}

	public String getFileName(String Idfilename, String Fileid)
	{   
		try{
			String line;
			FileInputStream fileidstream = new FileInputStream(Idfilename);
			BufferedReader fileidbr = new BufferedReader(new InputStreamReader(fileidstream));
			while ((line = fileidbr.readLine()) != null)   
			{  
				if(line.split(",")[1].matches(Fileid))
				{
					filename=line.split(",")[2];
					break;
				}
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		return filename;
	}

	public void Print(int[] array,PrintWriter out) {

		out.print("[");
		for (int v : array) {
			out.print("" + v + " ");
		}
		out.print("]");
	}

	public void Print(boolean[] array,PrintWriter out) {
		out.print("[");
		for (boolean v : array) {
			out.print(v ? "1" : "0");
		}
		out.print("]");
	}
}
