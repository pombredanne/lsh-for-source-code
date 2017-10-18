package persistencia;

import java.io.*;
import javax.crypto.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.IllegalBlockSizeException;
import java.security.*;
import java.security.MessageDigest;

public class DesEncrypter {

    Cipher ecipher;

    Cipher dcipher;

    private static String dirExercicios = "arquivos/exercicios/";

    private static String dirChaves = "arquivos/chaves/";

    public DesEncrypter(SecretKey key) {
        try {
            ecipher = Cipher.getInstance("des");
            dcipher = Cipher.getInstance("des");
            ecipher.init(Cipher.ENCRYPT_MODE, key);
            dcipher.init(Cipher.DECRYPT_MODE, key);
        } catch (javax.crypto.NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (java.security.InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public DesEncrypter(String arqChave) {
        try {
            ObjectInputStream keyIn = new ObjectInputStream(new FileInputStream(arqChave));
            SecretKey key = (SecretKey) keyIn.readObject();
            keyIn.close();
            ecipher = Cipher.getInstance("des");
            dcipher = Cipher.getInstance("des");
            ecipher.init(Cipher.ENCRYPT_MODE, key);
            dcipher.init(Cipher.DECRYPT_MODE, key);
        } catch (javax.crypto.NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (java.security.InvalidKeyException e) {
            e.printStackTrace();
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        } catch (java.lang.ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String encrypt(String str) {
        try {
            byte[] utf8 = str.getBytes("UTF8");
            byte[] enc = ecipher.doFinal(utf8);
            return new sun.misc.BASE64Encoder().encode(enc);
        } catch (javax.crypto.BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decrypt(String str) {
        try {
            byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);
            byte[] utf8 = dcipher.doFinal(dec);
            return new String(utf8, "UTF8");
        } catch (javax.crypto.BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String hash(String toEncripty) {
        if (toEncripty != null) {
            try {
                synchronized (toEncripty) {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(toEncripty.getBytes());
                    byte[] hash = md.digest();
                    StringBuffer hexString = new StringBuffer();
                    for (int i = 0; i < hash.length; i++) {
                        if ((0xff & hash[i]) < 0x10) hexString.append("0" + Integer.toHexString((0xFF & hash[i]))); else hexString.append(Integer.toHexString(0xFF & hash[i]));
                    }
                    toEncripty = hexString.toString();
                }
            } catch (Exception e) {
                e.getMessage();
            }
        }
        return toEncripty;
    }

    public static String encriptarString(String entrada, String chave) {
        try {
            DesEncrypter encrypter = new DesEncrypter(chave);
            return encrypter.encrypt(entrada);
        } catch (Exception e) {
            System.out.println("****Erro - Exception");
            return null;
        }
    }

    public static String decriptarString(String entrada, String chave) {
        try {
            DesEncrypter decrypter = new DesEncrypter(chave);
            return decrypter.decrypt(entrada);
        } catch (Exception e) {
            System.out.println("****Erro - Exception");
            return null;
        }
    }

    public static void encriptarArquivo(String arqEntrada, String arqSaida, String chave) {
        try {
            DesEncrypter encrypter = new DesEncrypter(chave);
            BufferedReader ent = new BufferedReader(new FileReader(arqEntrada));
            PrintWriter saida = new PrintWriter(new FileWriter(arqSaida));
            String linha = ent.readLine(), encrypted;
            while (linha != null) {
                encrypted = encrypter.encrypt(linha);
                saida.println(encrypted);
                linha = ent.readLine();
            }
            ent.close();
            saida.close();
        } catch (FileNotFoundException e) {
            System.out.println("****Erro - Arquivo de entrada nao encontrado");
        } catch (IOException e) {
            System.out.println("****Erro - IOException");
        }
    }

    public static void decriptarArquivo(String arqEntrada, String arqSaida, String chave) {
        try {
            DesEncrypter decrypter = new DesEncrypter(chave);
            BufferedReader ent = new BufferedReader(new FileReader(arqEntrada));
            PrintWriter saida = new PrintWriter(new FileWriter(arqSaida));
            String linha = ent.readLine(), decrypted;
            while (linha != null) {
                decrypted = decrypter.decrypt(linha);
                saida.println(decrypted);
                linha = ent.readLine();
            }
            ent.close();
            saida.close();
        } catch (FileNotFoundException e) {
            System.out.println("****Erro - Arquivo de entrada nao encontrado");
        } catch (IOException e) {
            System.out.println("****Erro - IOException");
        }
    }

    /**
     * Este procedimento decripta todo um arquivo gravando-o em outro cujos
     * caminhos (nome) sao especificados como argumento. A criptografia ser�
     * realizada com a chave especificada.
     **/
    public static String decriptarArquivo(String arqEntrada, String chave) {
        try {
            DesEncrypter decrypter = new DesEncrypter(chave);
            BufferedReader ent = new BufferedReader(new FileReader(arqEntrada));
            String saida = "";
            String linha = ent.readLine(), decrypted;
            while (linha != null) {
                decrypted = decrypter.decrypt(linha);
                saida.concat(decrypted);
                linha = ent.readLine();
            }
            ent.close();
            return saida;
        } catch (FileNotFoundException e) {
            System.out.println("****Erro - Arquivo de entrada nao encontrado");
            return null;
        } catch (IOException e) {
            System.out.println("****Erro - IOException");
            return null;
        }
    }

    /**
     * Este procedimento decripta todo um arquivo gravando-o em outro cujos
     * caminhos (nome) sao especificados como argumento. A criptografia ser�
     * realizada com a chave especificada. Nao serao cifradas linhas sem nenhum
     * caracter.
     **/
    public static void decriptarArquivoMenosLinhaVazia(String arqEntrada, String arqSaida, String chave) {
        try {
            DesEncrypter decrypter = new DesEncrypter(chave);
            BufferedReader ent = new BufferedReader(new FileReader(arqEntrada));
            PrintWriter saida = new PrintWriter(new FileWriter(arqSaida));
            String linha = ent.readLine(), decrypted;
            while (linha != null) {
                if (linha.length() == 0) {
                    saida.println(linha);
                    linha = ent.readLine();
                } else {
                    decrypted = decrypter.decrypt(linha);
                    saida.println(decrypted);
                    linha = ent.readLine();
                }
            }
            ent.close();
            saida.close();
        } catch (FileNotFoundException e) {
            System.out.println("****Erro - Arquivo de entrada nao encontrado");
        } catch (IOException e) {
            System.out.println("****Erro - IOException");
        }
    }

    /**
     * Este procedimento encripta todo um arquivo gravando-o em outro cujos
     * caminhos (nome) sao especificados como argumento. A criptografia ser�
     * realizada com a chave especificada. Nao serao cifradas linhas sem nenhum
     * caracter.
     **/
    public static void encriptarArquivoMenosLinhaVazia(String arqEntrada, String arqSaida, String chave) {
        try {
            DesEncrypter encrypter = new DesEncrypter(chave);
            BufferedReader ent = new BufferedReader(new FileReader(arqEntrada));
            PrintWriter saida = new PrintWriter(new FileWriter(arqSaida));
            String linha = ent.readLine(), encrypted;
            while (linha != null) {
                if (linha.length() == 0) {
                    saida.println(linha);
                    linha = ent.readLine();
                } else {
                    encrypted = encrypter.encrypt(linha);
                    saida.println(encrypted);
                    linha = ent.readLine();
                }
            }
            ent.close();
            saida.close();
        } catch (FileNotFoundException e) {
            System.out.println("****Erro - Arquivo de entrada nao encontrado");
        } catch (IOException e) {
            System.out.println("****Erro - IOException");
        }
    }

    /**
     * Gera chave secreta usada para criptografia com algoritmo des.
     * @param arquivo - Nome do arquivo que ser� gravado as chaves
     * @param seed - "Semente" ou frase-senha para gera��o da chave
     * @return Retorna True caso tenha sido bem sucedida a gera��o do par
     * de chaves 
     **/
    private static boolean gerarChaves(String arquivo, String seed) {
        boolean gera = false;
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("des");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            random.setSeed(seed.getBytes());
            keyGen.init(56, random);
            SecretKey chave = keyGen.generateKey();
            ObjectOutputStream key = new ObjectOutputStream(new FileOutputStream(arquivo + ".key"));
            key.writeObject(chave);
            key.close();
            gera = true;
        } catch (Exception e1) {
            gera = false;
            System.out.println("Exce��o: " + e1);
        }
        return (gera);
    }

    /** 
    * Gera chave secreta usada para criptografia com algoritmo des
    * @param arquivo - Nome do arquivo que ser� gravado as chaves
    * @param seed - "Semente" ou frase-senha para gera��o da chave
    * @return Retorna True caso tenha sido bem sucedida a gera��o do par
    * de chaves
    **/
    public static boolean setGeraChaves(String arquivo, String seed) {
        return gerarChaves(arquivo, seed);
    }

    /** 
    * Gera chave secreta usada para criptografia com algoritmo des
    * @param arquivo - Nome do arquivo que ser� gravado as chaves
    * @param seed - "Semente" ou frase-senha para gera��o da chave
    * @return Retorna True caso tenha sido bem sucedida a gera��o do par
    * de chaves
    **/
    public static boolean setGeraChaves(File arquivo, String seed) {
        return gerarChaves(arquivo.toString(), seed);
    }

    public static void main(String args[]) {
        try {
            encriptarArquivoMenosLinhaVazia(dirExercicios + "lmd.txt", dirExercicios + "lmd.des", dirChaves + "chavesdes.key");
            decriptarArquivoMenosLinhaVazia(dirExercicios + "lmd.des", dirExercicios + "lmd.decript", dirChaves + "chavesdes.key");
            encriptarArquivoMenosLinhaVazia(dirExercicios + "lmep.txt", dirExercicios + "lmep.des", dirChaves + "chavesdes.key");
            decriptarArquivoMenosLinhaVazia(dirExercicios + "lmep.des", dirExercicios + "lmep.decript", dirChaves + "chavesdes.key");
            encriptarArquivoMenosLinhaVazia(dirExercicios + "lmto.txt", dirExercicios + "lmto.des", dirChaves + "chavesdes.key");
            decriptarArquivoMenosLinhaVazia(dirExercicios + "lmto.des", dirExercicios + "lmto.decript", dirChaves + "chavesdes.key");
            encriptarArquivoMenosLinhaVazia(dirExercicios + "lmte.txt", dirExercicios + "lmte.des", dirChaves + "chavesdes.key");
            encriptarArquivoMenosLinhaVazia(dirExercicios + "ltct.txt", dirExercicios + "ltct.des", dirChaves + "chavesdes.key");
            decriptarArquivoMenosLinhaVazia(dirExercicios + "ltct.des", dirExercicios + "ltct.decript", dirChaves + "chavesdes.key");
            encriptarArquivoMenosLinhaVazia(dirExercicios + "lpct.txt", dirExercicios + "lpct.des", dirChaves + "chavesdes.key");
            decriptarArquivoMenosLinhaVazia(dirExercicios + "lpct.des", dirExercicios + "lpct.decript", dirChaves + "chavesdes.key");
            encriptarArquivoMenosLinhaVazia(dirExercicios + "sfd.txt", dirExercicios + "sfd.des", dirChaves + "chavesdes.key");
            decriptarArquivoMenosLinhaVazia(dirExercicios + "sfd.des", dirExercicios + "sfd.decript", dirChaves + "chavesdes.key");
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Testa se passou por aki");
        }
    }
}
