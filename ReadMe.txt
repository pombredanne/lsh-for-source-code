This project contains the source code which will apply locality sensitive hashing on the collections of files.
You should pass following parameters while running this project.

1. Tokenization file name: A text file that contains all the tokens and corresponding frequency of the files. 
Each line in tokenization file contains all the tokens and corresponding frequency of a file i.e 
if you have 2 files in collection there will be 2 lines in this tokenization file exactly in following format.

0,100250001@#@import@@::@@2,update@@::@@1,main@@::@@1,String@@::@@2
0,100250002@#@public@@::@@2,void@@::@@1,main@@::@@1,String@@::@@2

Here 0,100250001 is id of the file. and import is token and 2 is it's frequency.

2. Fileid File name: A text file that contains a mapping of the file id to it's file name. i.e for above tokenization file 
Fileid file will contain following information

0,100250001,C:bcb_reduced\3\default\100438.java
0,100250002,C:bcb_reduced\3\default\100777.java

so so 0,100250001 is the id of file 100438.java.

3. sparsity: a sparsity of your data. So based on this sparsity Boolean vector will be created. i.e if sparsity is 1 then for the file 100438.java 
a Boolean vector will be created as [1001] because for import frequency is 2 which is greater than 1. so Boolean is 1 and vice versa.

4.stages: LSH parameters the number of stages is also sometimes called the number of bands.

5.bucket: Number of bucket of LSA. to get relevant results, the number of elements per bucket should be at least 100.
		
		
For running this project execute following steps.

1. Download eclipse.

2.Set java path from my computer.

3.import project in eclipse.

4. go to run as-->run configuration-->Main-->

5.select your project name (LSH) from project.

6.select Main as Main class

7.go to run as-->run configuration-->Arguments

8.Enter following parameters in program arguments

<Tokenization file><Fileid File name><sparsity><stages><bucket>

9.for example 

Tokenfile.txt Fileid.txt 5 10 10

10.Enter -Xmx10240m in VM arguments(or memory you need if heap size/out of memory error occurs)

11.click Apply

12.Click run


The final output is generated in HashOutput.txt in following format.

file name:hash code















		
