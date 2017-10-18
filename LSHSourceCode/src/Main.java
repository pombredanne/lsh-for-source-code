import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.io.File;
public class Main  {
	public static LocSenHash obj=new LocSenHash();
	static void CreateDocTerMatrix (String filename)
	{
		LineNumberReader  lnr;
		String line;
		String localpos;
		String localposfile="localpos.txt";
		try{
			FileInputStream fstream = new FileInputStream(filename);
			PrintWriter localposwriter = new PrintWriter(localposfile, "UTF-8");
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			lnr = new LineNumberReader(new FileReader(new File(filename)));
			lnr.skip(Long.MAX_VALUE);
			obj.setNoofDoc(lnr.getLineNumber());
			lnr.close();
			while ((line = br.readLine()) != null)   
			{
				String fileid=line.split("@#@")[0];
				localposwriter.print(fileid.split(",")[1]+"#");
				String[] linearr=line.split("@#@")[1].split(",");
				int i=0;
				ArrayList<Integer> localposlist=new ArrayList<Integer>();
				while(i<linearr.length)
				{
					String[] linesubarr=linearr[i].split("@@::@@");
					if(obj.getTokenList().contains(linesubarr[0])==false)
						obj.getTokenList().add(linesubarr[0]);
					obj.setTokenList(obj.getTokenList());
					localposwriter.print(obj.getTokenList().indexOf(linesubarr[0])+":"+linesubarr[1]);
					localposwriter.print(",");
					localposlist.add(obj.getTokenList().indexOf(linesubarr[0]));		  
					i++;
				}
				obj.getAllTokenPosList().add(localposlist);
				obj.setAllTokenPosList(obj.getAllTokenPosList());
				localposwriter.print("\n");
			}
			br.close();
			localposwriter.close();
			obj.initDocTermMatr(obj.getNoofDoc(), obj.getTokenList().size());
			int [][] DocTermMatr=obj.getDocTermMatr();
			FileInputStream postream = new FileInputStream(localposfile);
			BufferedReader posbr = new BufferedReader(new InputStreamReader(postream));
			int m=0;
			while ((localpos = posbr.readLine()) != null)   
			{
				obj.getAllFileIdList().add(localpos.split("#")[0]);
				obj.setAllFileIdList(obj.getAllFileIdList());
				String[] localposarr=localpos.split("#")[1].split(",");
				int n=0;
				while(n<localposarr.length)
				{
					DocTermMatr[m][Integer.parseInt(localposarr[n].split(":")[0])]= 
							Integer.parseInt(localposarr[n].split(":")[1]);
					n++;
				}
				m++;
			}
			obj.setDocTermMatr(DocTermMatr);
		}
		catch(Exception e)
		{
			System.out.println(e);
		}

	}

	public static void main(String[] args) {
		String filename=args[0];
		//int[][] DocTermMatr=new int[nofodoc][tokenlist.size()];
		CreateDocTerMatrix(filename);
		//*****below is the comment from LSHExample file***//
		// proportion of 0's in the vectors
		// if the vectors are dense (lots of 1's), the average jaccard similarity
		// will be very high (especially for large vectors), and LSH
		// won't be able to distinguish them
		// as a result, all vectors will be binned in the same bucket...
		double sparsity = Double.parseDouble(args[2]);
		// Number of sets
		int count = obj.getNoofDoc() ;
		// Size of vectors
		int n = obj.getTokenList().size();
		// LSH parameters
		// the number of stages is also sometimes called the number of bands
		int stages = Integer.parseInt(args[3]);
		// Attention: to get relevant results, the number of elements per bucket
		// should be at least 100
		int buckets = Integer.parseInt(args[4]);
		boolean[][] vectors = new boolean[count][n];
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < n; j++) {
				vectors[i][j] =  obj.getsinglevalue(i, j)> sparsity;
			}
		}
		// Create and configure LSH algorithm
		LSHMinHash lsh = new LSHMinHash(stages, buckets, n);
		int[][] counts = new int[stages][buckets];
		// Perform hashing
		int id=0;
		try{
			PrintWriter out = new PrintWriter(new FileWriter("HashOutput.txt", false), false);
			out.println("The format of the output is"+" "+" "+"Filename"+":"+"Hash Code");
			out.println();
			out.println();
			for (boolean[] vector : vectors) {
				int[] hash = lsh.hash(vector);
				for (int i = 0; i < hash.length; i++) {
					counts[i][hash[i]]++;
				}
				out.print(obj.getFileName(args[1], 
						obj.getAllFileIdList().get(id)));
				out.print(" : ");
				obj.Print(hash,out);
				out.print("\n");
				id++;
			}
			out.println("Number of elements per bucket at each stage:");
			for (int i = 0; i < stages; i++) {
				obj.Print(counts[i],out);
				out.print("\n");
			}
			out.close();
			System.out.print("done"+"result is generated in HashOutput.txt");
		}
		catch(Exception e)
		{
			System.out.print(e);
		}

	}
}


