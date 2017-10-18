package br.org.ged.direto.model.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.io.FileInputStream;
import org.apache.commons.io.IOUtils;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.codec.Hex;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import br.org.direto.util.Base64Utils;
import br.org.direto.util.Config;
import br.org.ged.direto.model.entity.Anexo;
import br.org.ged.direto.model.entity.Carteira;
import br.org.ged.direto.model.entity.Despacho;
import br.org.ged.direto.model.entity.DocumentoDetalhes;
import br.org.ged.direto.model.entity.Historico;
import br.org.ged.direto.model.entity.Usuario;
import br.org.ged.direto.model.service.AnexoService;
import br.org.ged.direto.model.service.CarteiraService;
import br.org.ged.direto.model.service.DespachoService;
import br.org.ged.direto.model.service.DocumentosService;
import br.org.ged.direto.model.service.HistoricoService;
import br.org.ged.direto.model.service.SegurancaService;
import br.org.ged.direto.model.service.UsuarioService;
import java.util.Date;
import java.util.Scanner;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

@Service("segurancaService")
@RemoteProxy(name = "segurancaJS")
public class SegurancaServiceImpl implements SegurancaService {

    @Autowired
    private AnexoService anexoService;

    @Autowired
    private UsuarioService usuarioService;

    private Config config;

    @Autowired
    private CarteiraService carteiraService;

    @Autowired
    private HistoricoService historicoService;

    @Autowired
    private DespachoService despachoService;

    @Autowired
    private DocumentosService documentosService;

    private static final String signatureAlgorithm = "MD5withRSA";

    private final File jks;

    private final String pwd = "ZDE0M3Qw";

    private Cipher aes;

    private Cipher rsa;

    private SecretKeySpec aeskeySpec;

    public SegurancaServiceImpl(Config config) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.config = config;
        jks = new File(config.certificatesDir + "/store/truststore.jks");
        aes = Cipher.getInstance("AES");
        rsa = Cipher.getInstance("RSA/ECB/NoPadding");
        setAeskeySpec();
    }

    private void setAeskeySpec() {
        try {
            File chave = new File(config.certificatesDir + "/AES/direto.pk");
            if (chave.exists()) {
                aeskeySpec = getSecretKey();
            } else {
                Scanner in = new Scanner(System.in);
                System.out.println("Ainda não criado a chave AES.");
                System.out.print("Digite a senha do direto:_");
                String senha = in.nextLine();
                createSecretKey(senha);
                aeskeySpec = getSecretKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createSecretKey(String senha) throws Exception {
        PublicKey pk = getPublicKeyFromFile(new File(config.certificatesDir + "/store/truststore.jks"), "direto", new String(Base64Utils.decode(pwd.getBytes())));
        rsa.init(Cipher.ENCRYPT_MODE, pk);
        CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(config.certificatesDir + "/AES/direto.pk"), rsa);
        cos.write(senha.getBytes());
        cos.close();
    }

    private SecretKeySpec getSecretKey() throws Exception {
        File cert = new File(config.certificatesDir + "/direto.p12");
        PrivateKey pk = getPrivateKeyFromFile(cert, "direto", new String(Base64Utils.decode(pwd.getBytes())));
        rsa.init(Cipher.DECRYPT_MODE, pk);
        byte[] aesKey = new byte[32];
        CipherInputStream cis = new CipherInputStream(new FileInputStream(config.certificatesDir + "/AES/direto.pk"), rsa);
        cis.read(aesKey);
        cis.close();
        return new SecretKeySpec(md5(aesKey), "AES");
    }

    @Override
    @RemoteMethod
    public boolean encrypt(int idAnexo) {
        try {
            Anexo anexo = anexoService.selectById(idAnexo);
            aes.init(Cipher.ENCRYPT_MODE, aeskeySpec);
            FileInputStream fis = new FileInputStream(config.baseDir + "/arquivos_upload_direto/" + anexo.getAnexoCaminho());
            CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(config.baseDir + "/arquivos_upload_direto/encrypt/" + anexo.getAnexoCaminho()), aes);
            IOUtils.copy(fis, cos);
            cos.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    @RemoteMethod
    public boolean decrypt(int idAnexo) {
        try {
            Anexo anexo = anexoService.selectById(idAnexo);
            aes.init(Cipher.DECRYPT_MODE, aeskeySpec);
            CipherInputStream cis = new CipherInputStream(new FileInputStream(config.baseDir + "/arquivos_upload_direto/encrypt/" + anexo.getAnexoCaminho()), aes);
            FileOutputStream fos = new FileOutputStream(config.baseDir + "/arquivos_upload_direto/decrypt/" + anexo.getAnexoCaminho());
            IOUtils.copy(cis, fos);
            cis.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    @RemoteMethod
    public String encryptMessage(String message, String usuLogin) {
        try {
            Usuario destinatario = usuarioService.selectByLogin(usuLogin);
            PublicKey publicKey = getPublicKeyFromFile(jks, formatFileName(destinatario.getUsuIdt()), new String(Base64Utils.decode(pwd.getBytes())));
            rsa.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] bytes = message.getBytes("UTF-8");
            byte[] encrypted = blockCipher(bytes, Cipher.ENCRYPT_MODE);
            char[] encryptedTranspherable = Hex.encode(encrypted);
            String hexa = new String(encryptedTranspherable);
            return destinatario.getIdUsuario() + "," + hexa;
        } catch (Exception e) {
            e.printStackTrace();
            return "Ocorreu erro.";
        }
    }

    @Override
    @RemoteMethod
    public String decryptMessage(String password, int idDespacho) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Usuario decrypter = (Usuario) auth.getPrincipal();
            Despacho despacho = despachoService.selectDespacho(idDespacho);
            if (despacho.getIdUsuarioDestinatario() != decrypter.getIdUsuario()) return "Você não tem permissão para descriptografar essa mensagem.";
            File certificado = new File(config.certificatesDir + "/" + formatFileName(decrypter.getUsuIdt()) + ".p12");
            PrivateKey privateKey = getPrivateKeyFromFile(certificado, decrypter.getUsuLogin(), password);
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            System.out.println(despacho.getDespacho());
            byte[] bts = hexToBytes(despacho.getDespacho().toCharArray());
            byte[] decrypted = blockCipher(bts, Cipher.DECRYPT_MODE);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "ocorreu erro";
        }
    }

    public static byte[] hexToBytes(char[] hex) {
        int length = hex.length / 2;
        byte[] raw = new byte[length];
        for (int i = 0; i < length; i++) {
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            int value = (high << 4) | low;
            if (value > 127) value -= 256;
            raw[i] = (byte) value;
        }
        return raw;
    }

    private byte[] blockCipher(byte[] bytes, int mode) throws IllegalBlockSizeException, BadPaddingException {
        byte[] scrambled = new byte[0];
        byte[] toReturn = new byte[0];
        int length = (mode == Cipher.ENCRYPT_MODE) ? 100 : 128;
        byte[] buffer = new byte[length];
        for (int i = 0; i < bytes.length; i++) {
            if ((i > 0) && (i % length == 0)) {
                scrambled = rsa.doFinal(buffer);
                toReturn = append(toReturn, scrambled);
                int newlength = length;
                if (i + length > bytes.length) {
                    newlength = bytes.length - i;
                }
                buffer = new byte[newlength];
            }
            buffer[i % length] = bytes[i];
        }
        scrambled = rsa.doFinal(buffer);
        toReturn = append(toReturn, scrambled);
        return toReturn;
    }

    private byte[] append(byte[] prefix, byte[] suffix) {
        byte[] toReturn = new byte[prefix.length + suffix.length];
        for (int i = 0; i < prefix.length; i++) {
            toReturn[i] = prefix[i];
        }
        for (int i = 0; i < suffix.length; i++) {
            toReturn[i + prefix.length] = suffix[i];
        }
        return toReturn;
    }

    @Override
    public String md5(File arquivo) {
        return null;
    }

    public byte[] md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String md5(String texto) {
        String md5 = "";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(texto.getBytes()));
        md5 = hash.toString(16);
        return md5;
    }

    @Override
    public String sh1withRSA(File arquivo) throws FileNotFoundException, IOException {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        InputStream is = new FileInputStream(arquivo);
        byte[] buffer = new byte[8192];
        int read = 0;
        String output = "";
        try {
            while ((read = is.read(buffer)) > 0) {
                sha1.update(buffer, 0, read);
            }
            byte[] md5sum = sha1.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            output = bigInt.toString(16);
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
            }
        }
        return output;
    }

    @Override
    public String sh1withRSA(String texto) {
        String sh1 = "";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1withRSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(texto.getBytes()));
        sh1 = hash.toString(16);
        return sh1;
    }

    @Override
    public String sh1withRSA(InputStream is) throws FileNotFoundException, IOException {
        byte fileContent[] = IOUtils.toByteArray(is);
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, sha1.digest(fileContent));
        String sen = hash.toString(16);
        return sen;
    }

    public static byte[] getBytesFromFile(File file) throws IOException, FileNotFoundException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        System.out.println(length);
        if (length > Integer.MAX_VALUE) {
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    /**
	 * Extrai a chave pública do arquivo.
	 */
    private PrivateKey getPrivateKeyFromFile(File cert, String alias, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        char[] pwd = password.toCharArray();
        InputStream is = new FileInputStream(cert);
        ks.load(is, pwd);
        is.close();
        Key key = ks.getKey(alias, pwd);
        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }
        return null;
    }

    /**
	 * Extrai a chave pública do arquivo.
	 */
    private PublicKey getPublicKeyFromFile(File cert, String alias, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] pwd = password.toCharArray();
        InputStream is = new FileInputStream(cert);
        ks.load(is, pwd);
        Certificate c = (Certificate) ks.getCertificate(alias);
        PublicKey p = c.getPublicKey();
        return p;
    }

    /**
	 * Retorna a assinatura para o buffer de bytes, usando a chave privada.
	 * @param key PrivateKey
	 * @param buffer Array de bytes a ser assinado.
	 */
    private byte[] createSignature(PrivateKey key, byte[] buffer) throws Exception {
        Signature sig = Signature.getInstance(signatureAlgorithm);
        sig.initSign(key);
        sig.update(buffer, 0, buffer.length);
        return sig.sign();
    }

    /**
	 * Verifica a assinatura para o buffer de bytes, usando a chave pública.
	 * @param key PublicKey
	 * @param buffer Array de bytes a ser verficado.
	 * @param sgined Array de bytes assinado (encriptado) a ser verficado.
	 */
    public boolean verifySignature(PublicKey key, byte[] buffer, byte[] signed) throws Exception {
        Signature sig = Signature.getInstance(signatureAlgorithm);
        sig.initVerify(key);
        sig.update(buffer, 0, buffer.length);
        return sig.verify(signed);
    }

    /**
	 * Converte um array de byte em uma representação, em String, de seus hexadecimais.
	 */
    public String txt2Hexa(byte[] bytes) {
        if (bytes == null) return null;
        String hexDigits = "0123456789abcdef";
        StringBuffer sbuffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int j = ((int) bytes[i]) & 0xFF;
            sbuffer.append(hexDigits.charAt(j / 16));
            sbuffer.append(hexDigits.charAt(j % 16));
        }
        return sbuffer.toString();
    }

    @Override
    @RemoteMethod
    public String signFile(String alias, String password, int idAnexo) {
        try {
            Anexo anexoToSign = anexoService.selectById(idAnexo);
            if (anexoToSign.getAssinado() == 1) return "Arquivo já assinado.";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Usuario signer = (Usuario) auth.getPrincipal();
            File certificado = new File(config.certificatesDir + "/" + formatFileName(signer.getUsuIdt()) + ".p12");
            File fileToSing = new File(config.baseDir + "/arquivos_upload_direto/" + anexoToSign.getAnexoCaminho());
            FileInputStream fis = new FileInputStream(fileToSing);
            byte fileContent[] = new byte[(int) fileToSing.length()];
            fis.read(fileContent);
            PrivateKey privateKey = getPrivateKeyFromFile(certificado, alias, password);
            byte[] fileSigned = createSignature(privateKey, fileContent);
            String fileSignedHexa = txt2Hexa(fileSigned);
            System.out.println(fileSignedHexa);
            anexoToSign.setAssinado(1);
            anexoToSign.setAssinadoPor(signer.getUsuLogin());
            anexoToSign.setIdAssinadoPor(signer.getIdUsuario());
            anexoToSign.setAssinaturaHash(fileSignedHexa);
            anexoService.saveAnexo(anexoToSign);
            return "Documento assinado com sucesso!";
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("Arquivo nao encontrado.");
            return "Certificado digital nao encontrado!";
        } catch (IOException e) {
            e.printStackTrace();
            return "Erro de I/O!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Não foi possível assinar o documento!";
        }
    }

    @Override
    @RemoteMethod
    public boolean checkSignature(InputStream is, int idAnexo) {
        try {
            Anexo anexo = anexoService.selectById(idAnexo);
            if (anexo.getAssinado() == 0) return false;
            Usuario signerUser = usuarioService.selectById(anexo.getIdAssinadoPor());
            byte[] hash = new BigInteger(anexo.getAssinaturaHash(), 16).toByteArray();
            byte pwdDecripto[] = Base64Utils.decode(pwd.getBytes());
            byte[] fileContent = IOUtils.toByteArray(is);
            PublicKey publicKey = getPublicKeyFromFile(jks, formatFileName(signerUser.getUsuIdt()), new String(pwdDecripto));
            if (verifySignature(publicKey, fileContent, hash)) {
                System.out.println("Assinatura OK!");
                return true;
            } else {
                System.out.println("Assinatura NOT OK!");
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("Arquivo nao encontrado.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public String formatFileName(int idt) {
        String fileName = String.valueOf(idt);
        int sizeFileName = 10 - fileName.length();
        if (sizeFileName != 0) {
            String zeros = "";
            for (int i = 0; i < sizeFileName; i++) zeros += "0";
            fileName = zeros + fileName;
        }
        System.out.println(fileName);
        return fileName;
    }

    @Override
    @RemoteMethod
    public boolean haveCertificate(int usuIdt) {
        File certificado = new File(config.certificatesDir + "/" + formatFileName(usuIdt) + ".p12");
        if (certificado.exists()) return true;
        return false;
    }

    @Override
    @RemoteMethod
    public String blockEditDocument(int idAnexo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario signer = (Usuario) auth.getPrincipal();
        Anexo anexoToSign = anexoService.selectById(idAnexo);
        if (anexoToSign.getAssinado() == 1) return "Documento já assinado.";
        String sha1;
        try {
            File file = new File(config.baseDir + "/arquivos_upload_direto/" + anexoToSign.getAnexoCaminho());
            sha1 = sh1withRSA(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "Arquivo não encontrado";
        } catch (IOException e) {
            e.printStackTrace();
            return "erro";
        }
        anexoToSign.setAssinado(1);
        anexoToSign.setAssinadoPor(signer.getUsuLogin());
        anexoToSign.setIdAssinadoPor(signer.getIdUsuario());
        anexoToSign.setAssinaturaHash(sha1);
        anexoService.saveAnexo(anexoToSign);
        return "1";
    }

    @Override
    @RemoteMethod
    public String releaseDocumentEdition(int idAnexo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) auth.getPrincipal();
        Carteira carteira = carteiraService.selectById(usuario.getIdCarteira());
        Anexo anexo = anexoService.selectById(idAnexo);
        if (anexo.getIdAssinadoPor() != usuario.getIdUsuario()) return "Você não tem permissão para liberar a edição deste documento.";
        anexo.setAssinado(0);
        anexo.setAssinadoPor("");
        anexo.setIdAssinadoPor(0);
        anexoService.saveAnexo(anexo);
        String txtHistorico = "(Edição Liberada)-" + anexo.getAnexoNome() + "-";
        txtHistorico += usuario.getUsuLogin();
        Historico historico = new Historico();
        historico.setCarteira(carteira);
        historico.setDataHoraHistorico(new Date());
        historico.setHistorico(txtHistorico);
        historico.setDocumentoDetalhes(anexo.getDocumentoDetalhes());
        historico.setUsuario(usuario);
        historicoService.save(historico);
        return "Documento com edição liberada!";
    }

    @Override
    @RemoteMethod
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String releaseDocumentBlock(int idDocumentoDetalhes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) auth.getPrincipal();
        Carteira carteira = carteiraService.selectById(usuario.getIdCarteira());
        DocumentoDetalhes doc = documentosService.getDocumentoDetalhes(idDocumentoDetalhes);
        if (!doc.getAssinadoPor().equals(usuario.getUsuLogin())) return "Você não tem permissão para desbloquear este documento.";
        doc.setAssinatura(0);
        Set<Anexo> anexos = doc.getAnexos();
        for (Anexo anexo : anexos) anexo.setAssinado(0);
        String txtHistorico = "(Doc desbloqueado)-Todos os anexos foram desbloqueados-";
        txtHistorico += usuario.getUsuLogin();
        Historico historico = new Historico();
        historico.setCarteira(carteira);
        historico.setDataHoraHistorico(new Date());
        historico.setHistorico(txtHistorico);
        historico.setDocumentoDetalhes(doc);
        historico.setUsuario(usuario);
        historicoService.save(historico);
        return "Documento desbloqueado!";
    }
}
