package br.nic.connector.general;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.SocketFactory;
import br.nic.connector.dao.PagesDAO;
import br.nic.connector.database.HibernateFactory;
import br.nic.connector.database.Paginas;
import br.nic.connector.database.Sitios;

/**
 * Class with many different generic funtions used along the program.
 * @author Pedro Hadek
 * @author heitor
 */
public class Utils {

    /**
	 * Utilizando uma lista do site da ICANN, lista todos os TLDs (ccTLd ou gTLD) válidos.
	 * Em seguida
	 * @param tldFile
	 * local onde se encontra a lista sobre a qual a análise será feita.
	 * @param domList 
	 * Lista de todos os domínios
	 * @return
	 */
    public static List<String> buildTLDList(String tldFile, List<String> domList) {
        List<String> tlds = new ArrayList<String>();
        File tldList = new File(tldFile);
        domList.add(null);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(tldList));
            br.readLine();
            while (br.ready()) {
                tlds.add(br.readLine().toLowerCase());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Erro! Arquivo de lista de TLDs inválido - File Not Found");
            return null;
        } catch (IOException e) {
            System.out.println("Erro! Arquivo de lista de TLDs inválido - IOException");
            return null;
        } finally {
            if (br != null) try {
                br.close();
            } catch (IOException e) {
                SimpleLog.getInstance().writeException(e, 7);
            }
        }
        List<String> output = new ArrayList<String>();
        output.add(Constants.CONS_TYPES_GERAL);
        output.add(Constants.CONS_TYPES_TOP1000);
        output.add("br");
        output.add("com");
        for (Iterator<String> itTLDs = tlds.iterator(); itTLDs.hasNext(); ) {
            String tld = itTLDs.next();
            for (Iterator<String> itDom = domList.iterator(); itDom.hasNext(); ) {
                String dom = itDom.next();
                if (dom != null && dom.endsWith(tld)) {
                    if (!output.contains(tld)) output.add(tld);
                    break;
                }
            }
        }
        return output;
    }

    /**
	 * Copia todos os dados de um arquivo para o outro.
	 * @param originalFile
	 * Arquivo de onde virão os dados.
	 * @param copyFile
	 * Arquivo para onde irão os dados.
	 * @return
	 * true se a cópia teve sucesso, false caso contrário.
	 */
    public static boolean copyFile(File originalFile, File copyFile) {
        Utils.makeDirs(copyFile);
        InputStream entrada;
        try {
            entrada = new FileInputStream(originalFile);
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(copyFile));
            int count;
            byte[] buff = new byte[64 * 1024];
            while ((count = entrada.read(buff)) != -1) {
                fos.write(buff, 0, count);
            }
            fos.close();
            entrada.close();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
	 * Creates a "proportional map".
	 * @param totalListA
	 * @param totalListB
	 * @param dataListA
	 * @param dataListB
	 * @return
	 */
    public static Map<Long, Float[]> createDualProportionMap(float totalListA, float totalListB, Map<Long, Long> dataListA, Map<Long, Long> dataListB) {
        boolean listANull = dataListA == null || dataListA.isEmpty();
        boolean listBNull = dataListB == null || dataListB.isEmpty();
        if (listANull && listBNull) return null;
        if (listANull && !listBNull) {
            dataListA = dataListB;
            totalListA = totalListB;
            dataListB = new HashMap<Long, Long>();
            totalListB = 0;
        }
        Map<Long, Float[]> output = new LinkedHashMap<Long, Float[]>();
        List<Long> keysListA = new ArrayList<Long>(dataListA.keySet());
        List<Long> keysListB = new ArrayList<Long>(dataListB.keySet());
        long minListA = (long) (totalListA / 200);
        long minListB = (long) (totalListB / 200);
        if (minListA == 0) minListA = 1;
        if (minListB == 0) minListB = 1;
        int listIndexA = 0;
        int listIndexB = 0;
        while (true) {
            if (listIndexA >= keysListA.size() && listIndexB >= keysListB.size()) {
                break;
            }
            long currTotalA = 0;
            long currTotalB = 0;
            long indexA = 0;
            long indexB = 0;
            while (currTotalA < minListA && listIndexA < keysListA.size()) {
                indexA = keysListA.get(listIndexA);
                currTotalA += dataListA.get(indexA);
                listIndexA++;
            }
            while (currTotalB < minListB && listIndexB < keysListB.size()) {
                indexB = keysListB.get(listIndexB);
                listIndexB++;
                currTotalB += dataListB.get(indexB);
            }
            if (indexA < indexB) {
                while (listIndexA < keysListA.size()) {
                    indexA = keysListA.get(listIndexA);
                    if (indexA > indexB) break;
                    listIndexA++;
                    currTotalA += dataListA.get(indexA);
                }
            } else if (indexB < indexA) {
                while (listIndexB < keysListB.size()) {
                    indexB = keysListB.get(listIndexB);
                    if (indexB > indexA) break;
                    listIndexB++;
                    currTotalB += dataListB.get(indexB);
                }
            }
            long maxIndex;
            if (indexA > indexB) maxIndex = indexA; else maxIndex = indexB;
            Float[] results = { currTotalA / totalListA, currTotalB / totalListB };
            output.put(maxIndex, results);
        }
        return output;
    }

    /**
	 * Cria um "runscript.sh" no path padrão; sincronizado para impedir conflitos na criação do arquivo.
	 * @return
	 */
    public static synchronized boolean createRunscript() {
        File f = new File("runscript.sh");
        if (!f.exists()) {
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(f));
                writer.write(Constants.RUNSCRIPT);
                writer.close();
                f.setExecutable(true);
            } catch (IOException e) {
                SimpleLog.getInstance().writeLog(3, "Falha na criação do script de execução de rotinas externas");
            }
        }
        return f.exists();
    }

    /**
	 * Efetua o download de um arquivo e salva-o no local especificado.
	 * @param address 
	 * O endereço web do arquivo que se deseja baixar
	 * @param hostPath 
	 * O nome do host da página.
	 * @param reDownload
	 * boolean indicando se uma página que já está salva na estrutura de arquivos
	 * deve ser coletada novamente.
	 * @param rootFolder
	 * O local onde o arquivo será salvo
	 * @param id
	 * ID do arquivo no Banco de dados.
	 * @return
	 * O código http de resposta vindo do servidor.
	 */
    public static int downloadPage(String address, String hostPath, boolean reDownload, String rootFolder, long id) {
        String fileName = "";
        try {
            if (address.startsWith("http://")) {
                address = address.substring("http://".length());
            }
            fileName = makeFilePath(address, hostPath, rootFolder, id);
            File savedFile = new File(fileName);
            if (savedFile.exists() && savedFile.isDirectory()) savedFile = new File(fileName + ".igual");
            if (reDownload || !savedFile.exists()) {
                if (!savedFile.exists()) {
                    makeDirs(savedFile);
                }
                try {
                    savedFile.createNewFile();
                } catch (IOException e) {
                    savedFile = renamePath(hostPath, id);
                }
                int responseCode = httpDownload(address, savedFile);
                if ((responseCode < Constants.HTTP_OK || responseCode > 300) && savedFile.exists()) {
                    savedFile.delete();
                }
                if (responseCode >= Constants.HTTP_OK && responseCode < 300 && savedFile.exists() && savedFile.length() == 0) {
                    if (!savedFile.delete()) {
                        SimpleLog.getInstance().writeLog(7, "Aviso! Arquivo não foi salvo");
                    }
                    savedFile = renamePath(hostPath, id);
                    responseCode = httpDownload(address, savedFile);
                }
                return responseCode;
            }
            return Constants.HTTP_OK;
        } catch (IOException e) {
            SimpleLog.getInstance().writeLog(3, e.getMessage());
            return Constants.HTTP_ERROR_CONNECT;
        } catch (StringIndexOutOfBoundsException e) {
            SimpleLog.getInstance().writeLog(3, "Erro na formação da URL: " + address);
            SimpleLog.getInstance().writeException(e, 7);
            return Constants.HTTP_ERROR_REQUESTING;
        }
    }

    /**
	 * Download a page (from host) while following HTML redirects. The page will be saved to
	 * saveFile. Returns true if the download was successfull, false otherwise.
	 */
    public static boolean downloadWithRedirect(String host, File saveFile) {
        if (host == null) return false;
        if (host.contains("://")) {
            String[] split = host.split("://");
            if (!split[0].equals("http") && !split[0].contains("/")) {
                SimpleLog.getInstance().writeLog(6, "Cancelando download de " + host);
                return false;
            }
        }
        if (!host.startsWith("http://")) host = "http://" + host;
        int redirectLimit = 20;
        boolean redirect = true;
        boolean valid = true;
        boolean switchedAddress = false;
        String address = host;
        while (redirect && redirectLimit > 0) {
            redirectLimit--;
            valid = false;
            redirect = false;
            BufferedReader reader = null;
            BufferedWriter writer = null;
            int httpResult = Constants.HTTP_ERROR_UNKNOWN;
            try {
                URL u = new URL(host);
                URLConnection uc = u.openConnection();
                uc.setReadTimeout(Constants.DEFAULT_TIMEOUT);
                List<String> responseStatus = uc.getHeaderFields().get(null);
                String statusLine = "";
                for (String string : responseStatus) {
                    statusLine += string;
                }
                httpResult = Utils.getHttpResponseCode(statusLine);
                if (Utils.httpResponseSuccessful(httpResult)) {
                    reader = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                    writer = new BufferedWriter(new FileWriter(saveFile));
                    while (reader.ready()) {
                        String line = reader.readLine();
                        writer.write(line + "\n");
                    }
                    String redirectURL = Utils.getHTMLRedirect(saveFile);
                    if (redirectURL == null) {
                        valid = false;
                        redirect = false;
                        SimpleLog.getInstance().writeLog(6, "Página inválida em " + host);
                    } else if (redirectURL.equals("")) {
                        valid = true;
                        redirect = false;
                    } else if (redirectURL.equals(address)) {
                        SimpleLog.getInstance().writeLog(6, "Página com redirect em loop em " + host);
                        valid = false;
                        redirect = false;
                    } else {
                        if (!redirectURL.startsWith("http://")) {
                            while (redirectURL.endsWith("/")) redirectURL = redirectURL.substring(0, redirectURL.length() - 1);
                            if (address.contains("?")) address = address.substring(0, address.indexOf("?"));
                            address = address.substring(0, address.lastIndexOf("/")) + "/" + redirectURL;
                        } else {
                            address = redirectURL;
                        }
                        valid = true;
                        redirect = true;
                    }
                }
            } catch (Exception e) {
                if (e instanceof SocketTimeoutException) {
                    SimpleLog.getInstance().writeLog(7, "TimeOut para: " + address);
                    httpResult = Constants.HTTP_ERROR_SLOW;
                } else if (e instanceof ConnectException) {
                    String message = e.getMessage();
                    if (message != null && message.contains("Connection timed out")) {
                        SimpleLog.getInstance().writeLog(7, "TimeOut em " + address);
                        httpResult = Constants.HTTP_ERROR_TIMEOUT;
                    } else if (message != null && message.contains("Connection refused")) {
                        SimpleLog.getInstance().writeLog(7, "Conexão recusada em " + address);
                        httpResult = Constants.HTTP_ERROR_CONNECTION_REFUSED;
                    } else {
                        SimpleLog.getInstance().writeLog(7, "Erro ao se conectar em " + address);
                        httpResult = Constants.HTTP_ERROR_CONNECT;
                    }
                } else if (e instanceof NoRouteToHostException) {
                    SimpleLog.getInstance().writeLog(7, "Rota para o endereço " + address + " não encontrada.");
                    httpResult = Constants.HTTP_ERROR_NO_ROUTE;
                } else if (e instanceof UnknownHostException) {
                    SimpleLog.getInstance().writeLog(7, "Erro! host desconhecido em " + address);
                    httpResult = Constants.HTTP_ERROR_DNS;
                } else if (e instanceof IOException) {
                    SimpleLog.getInstance().writeLog(7, "IO exception em: " + address);
                    httpResult = Constants.HTTP_ERROR_CONNECT;
                } else if (e instanceof NullPointerException) {
                    SimpleLog.getInstance().writeLog(7, "NullPointException para " + address);
                    httpResult = Constants.HTTP_ERROR_UNKNOWN;
                } else {
                    SimpleLog.getInstance().writeLog(7, "Erro desconhecido para " + address);
                    httpResult = Constants.HTTP_ERROR_UNKNOWN;
                }
            } finally {
                try {
                    if (reader != null) reader.close();
                    if (writer != null) writer.close();
                } catch (IOException e) {
                    SimpleLog.getInstance().writeException(e, 7);
                }
            }
            if (!valid && !switchedAddress) {
                if (host.startsWith("http://www.")) address = "http://" + host.substring(11); else address = "http://www." + host.substring(7);
                switchedAddress = true;
                redirect = true;
                valid = true;
            }
            if (redirect && redirectLimit > 0) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }
        if (redirectLimit <= 0) {
            SimpleLog.getInstance().writeLog(3, "A página " + host + " tem um redirect em loop ou" + " em mais de 20 passos.");
        }
        return (valid && redirectLimit > 0 && saveFile != null);
    }

    /**
	 * Obtem o CountryCode Top Level Domain a partir de um dominio fornecido.
	 * Caso não haja um country code, retorna "int", indicando um TLD
	 * internacional
	 */
    public static String getccTLD(String dominio) {
        if (dominio != null) {
            for (Iterator<String> itCountry = Constants.DEFAULT_DOMAINS_COUNTRYS.iterator(); itCountry.hasNext(); ) {
                String countryCode = itCountry.next();
                if (dominio.endsWith("." + countryCode)) {
                    return countryCode;
                }
            }
            return "int";
        }
        return null;
    }

    /**
	 * Generates a SimpleDateFormat to be used in the response tests.
	 */
    public static SimpleDateFormat getDateFormat() {
        DateFormatSymbols nfs = new DateFormatSymbols();
        DateFormatSymbols.getAvailableLocales();
        nfs.setShortWeekdays(Constants.SHORT_WEEKDAYS);
        nfs.setShortMonths(Constants.SHORT_MONTHS);
        return new SimpleDateFormat(Constants.DEFAULT_RFC1123_DATE_PATTERN, nfs);
    }

    /**
	 * Retorna o domínio do host fornecido. Parte-se do pressuposto de que todos
	 * os domínios analisados terminam com .br. Caso não seja este o caso, tenta
	 * encontrar a qual país pertence o host em questão. Caso não possua um
	 * código de país válido, considera que não há código de país, e que o
	 * último valor é o subdomínio. Caso possua, considera que o penúltimo,
	 * independentemente do nome, é um subdomínio e, portanto, os 3 últimos
	 * constituem um domínio.
	 * 
	 * @param host
	 * Host a ser testado
	 * @return Domínio do host testado.
	 */
    public static String getDomain(String subdomain) {
        String result = null;
        if (subdomain != null) {
            String[] domains = subdomain.split("\\.");
            if (domains.length == 4) if (domains[2].equals("gov")) result = domains[1] + "." + domains[2] + "." + domains[3];
            if (domains.length == 3) result = domains[1] + "." + domains[2]; else if (domains.length == 2) result = domains[1];
        }
        if (result != null && result.length() > Sitios.MAX_SUBDOMAIN_SIZE) {
            result = result.substring(0, Sitios.MAX_SUBDOMAIN_SIZE);
        }
        return result;
    }

    /**
	 * Obtêm a extensão associada ao nome fornecido. Caso o nome em questão não
	 * tenha nenhuma extensão, retorna tudo que se encontra depois do último "."
	 * deste. Isto acontecerá, por exemplo, com arquivos representando uma
	 * requisição de GET.
	 */
    public static String getExtension(String url) {
        if (url != null) {
            String[] splitName = url.split("\\.");
            if (splitName.length > 0) return splitName[splitName.length - 1];
        }
        return null;
    }

    /**
	 * Obtêm a extensão associada ao nome fornecido, ignorando caracteres após
	 * um "?". Ou seja, obtêm a extensão de urls passadas com parâmetros GET em
	 * seu nome. Também ignora indicações de extenção que não estejam após a
	 * última barra. Caso não haja extensão, retorna Constants.LINKS_NOEXTENSION
	 * Caso haja uma extensão vazia, retorna Constants.LINKS_NOEXTENSION
	 */
    public static String getExtMinusParams(String url) {
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf('?'));
        }
        if (url.endsWith("/")) {
            return Constants.LINKS_EXTENSION_NONE;
        } else if (url.contains("://")) {
            url = url.split("://")[1];
            if (!url.contains("/")) {
                return Constants.LINKS_EXTENSION_NONE;
            }
        }
        String[] splitSlash = url.split("/");
        String last = splitSlash[splitSlash.length - 1];
        if (!last.contains(".")) {
            return Constants.LINKS_EXTENSION_NONE;
        } else if (last.endsWith(".")) {
            return Constants.LINKS_EXTENSION_EMPTY;
        } else {
            String[] splitDot = last.split("\\.");
            return splitDot[splitDot.length - 1];
        }
    }

    /**
	 * Obtem a extensão de um nome de arquivo
	 * @param outputFile
	 * Nome de um arquivo (pode ser fictício)
	 */
    public static String getFileExtension(String outputFile) {
        String[] split = outputFile.split("\\.");
        String lastPart = split[split.length - 1];
        if (lastPart.contains("/")) return ""; else {
            return lastPart;
        }
    }

    /**
	 * Obtem o nome de um arquivo sem sua extensão
	 * @param outputFile
	 * Nome de um arquivo (pode ser fictício)
	 */
    public static String getFileName(String outputFile) {
        String[] split = outputFile.split("\\.");
        String lastPart = split[split.length - 1];
        if (lastPart.contains("/")) return outputFile; else {
            return outputFile.substring(0, outputFile.length() - lastPart.length() - 1);
        }
    }

    /**
	 * Obtêm o valor que identificará uma pasta onde será salvo um documento.
	 * Apenas documentos pertencentes à pasta de uma coleta podem ser
	 * utilizados.
	 * 
	 * @param Id
	 * Id gerado pelo wire para o host em questão
	 * @param folderDepth
	 * Quantidade de pastas onde os documentos serão separados.
	 * @return String no formato adequado para geração das pastas.
	 */
    public static String getFolderNum(Long Id, int folderDepth) {
        String val = "";
        long divisor = (long) Math.pow(Constants.DEFAULT_MAXFOLDERSIZE, folderDepth);
        for (int i = 0; i < folderDepth; i++) {
            val += (Id / divisor) % Constants.DEFAULT_MAXFOLDERSIZE + "_/";
            divisor = divisor / Constants.DEFAULT_MAXFOLDERSIZE;
        }
        return val;
    }

    /**
	 * Retorna o continente ao qual pertence uma dada geolocalização.
	 * OBS: O Brasil é considerado separado da América do Sul, por servir aos testes. 
	 */
    public static String getGeoContinent(String geo) {
        String output = Constants.CONS_CSV_GEO_OUTRO;
        if (geo == null) return output;
        if (geo.equals(Constants.DEFAULT_GEOLOCAL_BRASIL)) output = Constants.CONS_CSV_GEO_BRASIL; else if (Constants.DEFAULT_GEOLOCAL_AFRICA.contains(geo)) output = Constants.CONS_CSV_GEO_AFRICA; else if (Constants.DEFAULT_GEOLOCAL_AMNORTE.contains(geo)) output = Constants.CONS_CSV_GEO_AMNORTE; else if (Constants.DEFAULT_GEOLOCAL_AMSUL.contains(geo)) output = Constants.CONS_CSV_GEO_AMSUL; else if (Constants.DEFAULT_GEOLOCAL_ASIA.contains(geo)) output = Constants.CONS_CSV_GEO_ASIA; else if (Constants.DEFAULT_GEOLOCAL_EUROPA.contains(geo)) output = Constants.CONS_CSV_GEO_EUROPA; else if (Constants.DEFAULT_GEOLOCAL_OCEANIA.contains(geo)) output = Constants.CONS_CSV_GEO_OCEANIA;
        return output;
    }

    /**
	 * Retorna uma String de tamanho adequado representando um número
	 * HexaDecimal como resultado de uma função de Hash do tipo definido em
	 * Constants.DEFAULT_HASHFUNCTION.
	 */
    public static String getHash(String nome) {
        if (nome == null) return null;
        try {
            MessageDigest messageDgst = MessageDigest.getInstance(Constants.DEFAULT_HASHFUNCTION);
            messageDgst.reset();
            byte[] x = messageDgst.digest(nome.getBytes());
            StringBuilder sb = new StringBuilder(x.length);
            for (int i = 0; i < x.length; i++) {
                if (x[i] > 15) sb.append(Integer.toHexString(x[i])); else if (x[i] >= 0) {
                    sb.append("0");
                    sb.append(Integer.toHexString(x[i]));
                } else sb.append(Integer.toHexString(x[i]).substring(6));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Verifica se uma dada página possui um redirect em HTML.
	 * @param saveFile
	 * Local onde se encontra o arquivo a ser lido.
	 * @return
	 * String vazia - Indica que não tem Redirect
	 * String null - Indica que houve erro na detecção
	 * Outros - É o endereço para o qual deve-se redirecionar.
	 */
    public static String getHTMLRedirect(File saveFile) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(saveFile));
        } catch (FileNotFoundException e) {
        }
        if (br != null) {
            try {
                String output = "";
                boolean foundRedirect = false;
                boolean endedHead = false;
                for (String line = br.readLine(); line != null && !foundRedirect && !endedHead; line = br.readLine()) {
                    if (line.contains("<meta") && line.contains("http-equiv=\"REFRESH\"")) {
                        String[] split = line.split(";url=");
                        if (split.length > 1) {
                            output = split[1].substring(0, split[1].indexOf("\""));
                            foundRedirect = true;
                        }
                    }
                    if (line.contains("</head>")) endedHead = true;
                }
                br.close();
                return output;
            } catch (Exception e) {
            }
            try {
                br.close();
            } catch (IOException e) {
            }
        }
        return null;
    }

    /**
	 * Obtains the HttpResponseCode from the status response line.
	 * @param statusLine
	 * @return
	 */
    public static int getHttpResponseCode(String statusLine) {
        int http_status = Constants.HTTP_STATUS_UNDEFINED;
        String[] splitedLine = statusLine.split(" ");
        if (splitedLine[0].startsWith("HTTP") && splitedLine.length > 2) {
            try {
                http_status = Integer.parseInt(splitedLine[1]);
            } catch (NumberFormatException nfe) {
                http_status = 0;
            }
            if (http_status < 100) {
                http_status = Constants.HTTP_ERROR_PROTOCOL;
            }
        } else {
            http_status = Constants.HTTP_ERROR_PROTOCOL;
        }
        return http_status;
    }

    /**
	 * Retorna a estrutura de pastas, sem o nome final de um arquivo, onde uma certa página
	 * deve estar contida.
	 * @param pagina
	 * página cuja pasta se deseja encontrar.
	 * @return
	 * Caminho da pasta que contêm a página.
	 */
    public static String getPageFolder(String pagina) {
        if (pagina.contains("?")) {
            pagina = pagina.split("\\?")[0];
        }
        if (pagina.endsWith("/")) return pagina;
        String[] separa = pagina.split("/");
        String fim = separa[separa.length - 1];
        String aux = pagina.substring(0, pagina.length() - fim.length());
        return aux;
    }

    /**
	 * Obtem qual o nome do host da página da qual se forneceu apenas o nome.
	 */
    public static String getPageHost(String pageName) {
        if (pageName != null) {
            try {
                if (pageName.contains("?")) {
                    pageName = pageName.substring(0, pageName.indexOf('?'));
                }
                if (pageName.contains("://")) {
                    pageName = pageName.split("://+", 2)[1];
                }
                if (pageName.contains("/")) {
                    pageName = pageName.substring(0, pageName.indexOf("/"));
                }
                return pageName;
            } catch (Exception e) {
                System.out.println(e.getLocalizedMessage() + "\n" + "Page Name: " + pageName);
            }
        } else {
            SimpleLog.getInstance().writeLog(5, "PageName pra PageHost não pode ser null.");
        }
        return null;
    }

    /**
	 * Obtem qual o nome do host da página cujo Path absoluto foi fornecido.
	 */
    public static String getPageHost(String absolutePath, String rootPath) {
        return getPageHost(getPageName(absolutePath, rootPath));
    }

    /**
	 * Obtem o nome da página fornecida em absolutePath a partir da pasta
	 * dada em rootPath. Considera que o path base pode estar em diretórios a
	 * partir de rootPath, mas que nenhum deles, exceto o primeiro diretório do
	 * Host, terá um "." em seu nome. Encontrando o nome, faz um parsing neste,
	 * traduzindo certos padrões de caracteres, o que é necessário para se
	 * evitar erros, mas manter uma representação consistente dos nomes por todo
	 * o programa. As palavras reservadas, além das definidas em
	 * Constants.DEFAULT_RENAMES, são:<p>
	 * 
	 * [renamed]número: esta deve ser a última parte nome do arquivo inteiro.
	 * Neste caso, irá trocar o nome por um outro nome relacionado no arquivo
	 * definido em Constants.DEFAULT_RENAMEDARCHIVE. O número funcionará como um
	 * ID no arquivo, traduzindo o nome para o nome correto. É utilizado quando
	 * o nome do arquivo é grande demais.<p>
	 * index.root: Caso o nome do Arquivo seja index.root, indica, que é um arquivo
	 * padrão retornado quando da chamada de uma pasta. Deve ser removido do nome.<p>
	 * Extensão .igual: Indica que existe um arquivo com este mesmo nome, em geral, se
	 * tratando de um arquivo e uma pasta com o mesmo nome (a pasta deverá ter um / no
	 * final). Deve ser removido do nome.
	 * 
	 * @param absolutePath
	 * Caminho absoluto, a partir do root do sistema, onde se encontra a página em questão
	 * @param rootPath
	 * Caminho absoluto, a partir do root do sistema, de onde as pastas que contem as páginas
	 * se originam.
	 * @return
	 * O nome da página, como deve aparecer no banco de dados.
	 */
    public static String getPageName(String absolutePath, String rootPath) {
        if (absolutePath == null) return null;
        String returnName = null;
        String[] basePath = absolutePath.split(rootPath, 2);
        if (basePath.length > 1) {
            String[] posRootPath = (basePath[1]).split("\\.");
            if (posRootPath.length > 1) {
                String[] baseHost = posRootPath[0].split("/");
                if (baseHost.length > 1) {
                    String base = baseHost[baseHost.length - 1];
                    String pagePath = basePath[1].split("/" + base + "\\.", 2)[1];
                    returnName = base + "." + pagePath;
                } else {
                    returnName = basePath[1];
                }
            } else {
                returnName = basePath[1];
            }
            boolean ok = true;
            do {
                ok = true;
                if (returnName.endsWith("index.r00t")) {
                    returnName = returnName.substring(0, returnName.length() - "index.r00t".length());
                    ok = false;
                }
                if (returnName.endsWith(".igual")) {
                    returnName = returnName.substring(0, returnName.length() - ".igual".length());
                    ok = false;
                }
                String[] splitRenamed = returnName.split("\\[renamed\\]");
                if (splitRenamed.length > 1) {
                    ok = false;
                    if (Constants.STATICS_RENAMED_LIST == null) {
                        startRenamedList(rootPath);
                    }
                    returnName = splitRenamed[0];
                    String nameID = "[renamed]" + splitRenamed[1];
                    if (Constants.STATICS_RENAMED_LIST.containsKey(nameID)) {
                        returnName += Constants.STATICS_RENAMED_LIST.get(nameID);
                        String[] tail = splitRenamed[1].split("\\d+", 2);
                        if (tail.length > 1) returnName += tail[1];
                    } else {
                        PagesDAO pagesInfo = new PagesDAO(new HibernateFactory().getSession(), false);
                        Paginas page = pagesInfo.getPageFromWireId(Long.parseLong(splitRenamed[1]));
                        if (page != null) {
                            returnName = page.getPagina();
                            ok = true;
                        } else {
                            returnName += "[renamed]" + splitRenamed[1];
                            ok = true;
                        }
                    }
                }
            } while (!ok);
            for (int i = 0; i < Constants.DEFAULT_RENAMES.length; i++) {
                try {
                    returnName = returnName.replaceAll(Constants.DEFAULT_RENAMES[i][0], Constants.DEFAULT_RENAMES[i][1]);
                } catch (Exception e) {
                    System.out.println("Nome com erro: " + returnName);
                }
            }
        }
        return returnName;
    }

    /**
	 * Gera um pathname para uma pasta pertencente a um domínio externo.
	 * 
	 * @param host
	 *            Endereço do site cuja pasta se deseja obter.
	 * @param rootFolder
	 *            Pasta raiz, a partir da qual todas as outras são obtidas.
	 * @param folderDepth
	 *            * @param folderDepth Profundidade esperada para a estrutura
	 *            das pastas.
	 * @return Path absoluto, baseado em rootFolder, onde os arquivos do host
	 *         fornecido deverão ser armazenados.
	 */
    public static String getPathNameExt(String host, String rootFolder, int folderDepth, long extHostID) {
        host = removeHttp(host);
        rootFolder = rootFolder.trim();
        while (rootFolder.charAt(rootFolder.length() - 1) == '/' || rootFolder.charAt(rootFolder.length() - 1) == '\\') {
            rootFolder = rootFolder.substring(0, rootFolder.length() - 1);
        }
        rootFolder += "/ext";
        String folderNum = getFolderNum(extHostID, folderDepth);
        String pathName = rootFolder + "/" + folderNum;
        pathName += host;
        return pathName;
    }

    /**
	 * Obtêm o path para uma pasta referente a um site pertencente ao escopo da
	 * coleta, ou seja, que possui um hostID
	 * 
	 * @param host
	 *   Endereço do site cuja pasta se deseja obter.
	 * @param rootFolder
	 *   Pasta raiz, a partir da qual todas as outras são obtidas.
	 * @param folderDepth
	 *   Profundidade esperada para a estrutura das pastas.
	 * @param hostID
	 *   ID do host fornecido, tal qual gerado pelo Wire.
	 * @return Path absoluto, baseado em rootFolder, onde os arquivos do host
	 *   fornecido deverão ser armazenados.
	 */
    public static String getPathNameLocal(String host, String rootFolder, int folderDepth, long hostID) {
        host = removeHttp(host);
        rootFolder = rootFolder.trim();
        while (rootFolder.charAt(rootFolder.length() - 1) == '/' || rootFolder.charAt(rootFolder.length() - 1) == '\\') {
            rootFolder = rootFolder.substring(0, rootFolder.length() - 1);
        }
        String folderNum = getFolderNum(hostID, folderDepth);
        String pathName = rootFolder + "/" + folderNum;
        pathName += host;
        return pathName;
    }

    /**
	 * Classifica o servidor indicado de acordo com tipos pré-definidos
	 * @param server
	 * Definição do servidor, como apresentada por uma query HTTP
	 * @param logOthers
	 * Indica se será efetuado um log dos servidores considerados como "Outros".
	 * @param value
	 * Valor utilizado na geração de alguns logs.
	 * @return
	 * Tipo de servidor identificado.
	 */
    public static String getServerType(String server, boolean logOthers, long value) {
        if (server == null) {
            return Constants.CONS_SERVER_NONE;
        }
        server = server.toLowerCase();
        String target = Constants.CONS_SERVER_OTHERS;
        boolean foundServer = false;
        boolean multiServers = false;
        for (Iterator<String> itServers = Constants.CONS_SERVER_KNOWNTYPE.iterator(); itServers.hasNext(); ) {
            String serverType = itServers.next();
            if (server.contains(serverType.toLowerCase())) {
                if (!foundServer) {
                    foundServer = true;
                    target = serverType;
                } else if (!target.contains(serverType) && !serverType.contains(target)) {
                    target = Constants.CONS_SERVER_OTHERS;
                    multiServers = true;
                }
            }
        }
        if (!foundServer) for (Iterator<String> itErrors = Constants.CONS_SERVER_UNKNOWNTYPE.iterator(); itErrors.hasNext() && !foundServer; ) {
            String error = itErrors.next();
            if (server.contains(error.toLowerCase())) {
                foundServer = true;
                target = Constants.CONS_SERVER_NONE;
            }
        }
        if (!foundServer) for (Iterator<String> itServers = Constants.CONS_SERVER_KNOWNOTHERS.iterator(); itServers.hasNext() && !foundServer; ) {
            String serverType = itServers.next();
            if (server.contains(serverType.toLowerCase())) {
                foundServer = true;
                target = Constants.CONS_SERVER_OTHERS;
            }
        }
        if (!foundServer) if (server.length() <= 2) {
            foundServer = true;
            target = Constants.CONS_SERVER_NONE;
        }
        if (!foundServer) if (server.contains("/")) {
            if (server.length() <= 2) {
                foundServer = true;
                target = Constants.CONS_SERVER_OTHERS;
                SimpleLog.getInstance().writeLog(3, "Server Other (Slash): " + value + " times - " + server);
            }
        }
        if (logOthers && !foundServer) SimpleLog.getInstance().writeLog(3, "Server Other: " + value + " times - " + server);
        if (logOthers && multiServers) SimpleLog.getInstance().writeLog(3, "Multiple Servers: " + value + " times - " + server);
        return target;
    }

    /**
	 * Obtem o domínio (ccTLD, TLD ou TLD.ccTLD) do domínio fornecido
	 * 
	 * @param host
	 *   host a ser analisado. Tudo após a primeira barra é ignorado.
	 * @return Top-Level Domain associado a este. Caso o domínio fornecido seja
	 *   inválido (número errado de componenets) ou null, retorna null.
	 */
    public static String getSubdomain(String host) {
        if (host.contains("/")) {
            host = host.split("/")[0];
        }
        String subDomain = "";
        String countryCode = "";
        boolean hasSubDomain = false;
        boolean hasCountryCode = false;
        if (host.endsWith(".br")) {
            hasCountryCode = true;
            countryCode = "br";
            host = host.split("\\.br$")[0];
            for (Iterator<String> itSub = Constants.DEFAULT_DOMAINS.iterator(); itSub.hasNext(); ) {
                subDomain = itSub.next();
                if (host.endsWith("." + subDomain)) {
                    host = host.split("\\." + subDomain)[0];
                    hasSubDomain = true;
                    break;
                }
            }
        } else {
            countryCode = getccTLD(host);
            if (countryCode.equals(Constants.LINKS_INTERNATIONAL)) {
                hasCountryCode = false;
                hasSubDomain = true;
                String[] splitSub = host.split("\\.");
                if (splitSub.length > 1) {
                    subDomain = splitSub[splitSub.length - 1];
                    host = host.substring(0, host.indexOf("." + subDomain));
                } else {
                    return null;
                }
            } else {
                hasCountryCode = true;
                host = host.split("\\." + countryCode + "$")[0];
                String[] splitSub = host.split("\\.");
                if (splitSub.length > 1) {
                    subDomain = splitSub[splitSub.length - 1];
                    host = host.substring(0, host.indexOf("." + subDomain));
                    if (host.equals("www")) {
                        hasSubDomain = false;
                        host = subDomain;
                    } else {
                        hasSubDomain = true;
                    }
                } else {
                    hasSubDomain = false;
                }
            }
        }
        String[] splitHost = host.split("\\.");
        String domain = (splitHost.length >= 1 ? splitHost[splitHost.length - 1] : host);
        if (hasSubDomain) {
            domain += "." + subDomain;
        }
        if (hasCountryCode) {
            domain += "." + countryCode;
        }
        return domain;
    }

    /**
	 * Obtêm o Top Level Domain do domínio fornecido, removendo o seu contryCode caso existente
	 */
    public static String getTLDDomain(String domain) {
        String[] splitDot = domain.split("\\.");
        if (splitDot.length == 1) return splitDot[0]; else if (Constants.DEFAULT_DOMAINS_COUNTRYS.contains(splitDot[splitDot.length - 1])) {
            String tld = "";
            for (int i = 0; i < splitDot.length - 1; i++) {
                tld += splitDot[i] + ".";
            }
            return tld.substring(0, tld.length() - 1);
        } else return domain;
    }

    /**
	 * Com base nas duas listas fornecidas, gera uma lista de hosts ainda não
	 * testados. Pode ser usada para outros fins, como obter os IPs ainda não
	 * testados.
	 * 
	 * @param hostsList
	 *   Lista dos hosts cujo teste desejamos realizar.
	 * @param testedHostsList
	 *   Lista com os hosts já testados quanto ao parâmetro desejado.
	 * @return Lista contendo todos os itens de hostsList que não constam em
	 *   testedHostsList, ou seja, todos os hosts ainda não testados.
	 */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<String> getUntestedHostsList(List<?> hostsList, List<String> testedHostsList) {
        List<String> untestedHostsList = new ArrayList(hostsList);
        for (Iterator<String> it = testedHostsList.iterator(); it.hasNext(); ) untestedHostsList.remove(it.next());
        Collections.shuffle(untestedHostsList);
        return untestedHostsList;
    }

    /**
	 * Obtem o path de uma pagina a partir de seu endereço
	 * @param address
	 * endereço da página. sem a adição do protocolo de conexão
	 * @return
	 * O string com o path da pagina.
	 */
    public static String getUrlPath(String address) {
        if (address.contains("://")) {
            address = address.split("://+", 2)[1];
            if (address.contains("/")) {
                address = address.substring(address.indexOf("/"), address.length());
                return address;
            } else return "/";
        } else if (address.contains("/")) return address.substring(address.indexOf('/')); else return "/";
    }

    /**
	 * Executa o download de um arquivo a partir de um dado endereço, salvando-o em um dado
	 * arquivo. Esta função apenas efetua o download utilizando o parâmetro GET do HTTP 1.1.
	 * Logo, não será possível utiliza-la para baixar de locais que exijam outro protocolo.
	 * @param address
	 * Endereço onde o arquivo a ser baixado se encontra.
	 * @param file
	 * Arquivo onde será salvo o arquivo baixado.
	 * @return
	 * Id do status retornado pelo servidor, conforme especificação do HTTP, ou um número indicando
	 * um possível erro interno do programa, como definido em Constants.HTTP_ERROR_*
	 * @throws IOException
	 */
    public static int httpDownload(String address, File file) {
        DataInputStream dis = null;
        BufferedOutputStream fos = null;
        Socket sock = null;
        InputStream rd = null;
        InetAddress ip = null;
        int response = Constants.HTTP_ERROR_UNKNOWN;
        try {
            String host = Utils.getPageHost(address);
            ip = InetAddress.getByName(host);
            sock = SocketFactory.getDefault().createSocket(ip, Constants.DEFAULT_PORT);
            sock.setSoTimeout(Constants.DEFAULT_TIMEOUT);
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "ISO-8859-1"));
            wr.write("GET " + Utils.getUrlPath(address) + " HTTP/1.1\r\n" + "Host: " + host + "\r\n" + "User-Agent: WIRE/1.0 (Linux;i686;Bot,Robot,Spider,Crawler)\r\n" + "Connection: close\r\n" + "Accept: text/html, text/plain" + "\r\n\r\n");
            wr.flush();
            rd = sock.getInputStream();
            response = parseHttpStatus(rd);
            if (httpResponseSuccessful(response)) {
                int size = 0;
                byte[] buff = new byte[16 * 1024];
                long initialTime = System.currentTimeMillis();
                fos = new BufferedOutputStream(new FileOutputStream(file));
                int count;
                dis = new DataInputStream(sock.getInputStream());
                while ((count = dis.read(buff)) != -1 && size < 500 * 1024 && System.currentTimeMillis() - initialTime <= 30000) {
                    fos.write(buff, 0, count);
                    size += count;
                }
            }
        } catch (SocketTimeoutException e) {
            SimpleLog.getInstance().writeLog(7, "TimeOut para: " + address);
            response = Constants.HTTP_ERROR_SLOW;
        } catch (ConnectException e) {
            String message = e.getMessage();
            if (message != null && message.contains("Connection timed out")) {
                SimpleLog.getInstance().writeLog(7, "TimeOut para: " + address);
                response = Constants.HTTP_ERROR_TIMEOUT;
            } else if (message != null && message.contains("Connection refused")) {
                SimpleLog.getInstance().writeLog(7, "conexão recusada: " + address);
                response = Constants.HTTP_ERROR_CONNECTION_REFUSED;
            } else {
                SimpleLog.getInstance().writeLog(7, "erro ao se conectar: " + address);
                response = Constants.HTTP_ERROR_CONNECT;
            }
        } catch (NoRouteToHostException e) {
            if (ip != null) SimpleLog.getInstance().writeLog(7, "Rota para o endereço " + address + " não encontrada para o IP " + ip); else SimpleLog.getInstance().writeLog(7, "No route to: " + address);
            response = Constants.HTTP_ERROR_NO_ROUTE;
        } catch (UnknownHostException e) {
            SimpleLog.getInstance().writeLog(7, "Erro! host desconhecido em " + address);
            response = Constants.HTTP_ERROR_DNS;
        } catch (IOException e) {
            SimpleLog.getInstance().writeLog(7, "IO exception em: " + address);
            response = Constants.HTTP_ERROR_CONNECT;
        } catch (NullPointerException e) {
            SimpleLog.getInstance().writeLog(7, "NullPointException para " + address);
            response = Constants.HTTP_ERROR_UNKNOWN;
        }
        try {
            if (fos != null) fos.close();
            if (dis != null) dis.close();
            if (rd != null) rd.close();
            if (sock != null) sock.close();
        } catch (IOException e) {
            SimpleLog.getInstance().writeLog(3, "Erro no fechamento dos streams para " + address);
        }
        return response;
    }

    /**
	 * Returns true if the responseCode indicates a page with a redirect, false otherwise.
	 */
    public static boolean httpResponseRedirect(int responseCode) {
        return responseCode >= 300 && responseCode < 400;
    }

    /**
	 * Returns true if the responseCode indicates a successfull page get, false otherwise.
	 * Ignores truncated pages. 
	 */
    public static boolean httpResponseSuccessful(int responseCode) {
        return responseCode >= 200 && responseCode < 300;
    }

    /**
	 * Retorna a sequência de bits de um número passado como parâmetro
	 * @param value
	 * Valor a ser convertido
	 * @param minSize
	 * Tamanho mínimo do vetor retornado. Mesmo se não houverem mais valores a serem convertidos,
	 * preenche a lista com 0's.
	 * @return
	 * Vetor com os bits, onde na posição 0 se encontra o mais significativo (little-endian)
	 */
    public static Byte[] intToBitArray(int value, int minSize) {
        List<Byte> list = new ArrayList<Byte>();
        char[] bits = Integer.toBinaryString(value).toCharArray();
        for (int i = 0; i < bits.length; i++) {
            list.add((byte) (Integer.valueOf("" + bits[i]) + 0));
        }
        for (int i = list.size(); i < minSize; i++) {
            list.add((byte) 0);
        }
        Byte[] array = {};
        return list.toArray(array);
    }

    /**
	 * Verifies if the URL of a link is local or not.
	 */
    public static boolean isLinkLocal(String url) {
        if (url.contains("?")) url = url.substring(0, url.indexOf('?'));
        if (url.contains("://")) return false; else return true;
    }

    /**
	 * Tenta criar a sequencia de pastas que contem o arquivo targetFile.
	 * @return
	 * true se a criação teve sucesso, false caso contrário.
	 */
    public static synchronized boolean makeDirs(File targetFile) {
        String fileName = targetFile.getAbsolutePath();
        if (!fileName.contains("/")) return false;
        if (fileName.endsWith("/")) fileName = fileName.substring(0, fileName.length() - 1);
        String folderName = fileName.substring(0, fileName.lastIndexOf("/") + 1);
        if (!folderName.endsWith("/") || folderName.lastIndexOf('/') <= 0) return false;
        File folder = new File(fileName.substring(0, fileName.lastIndexOf("/") + 1));
        if (folder.exists() && folder.isDirectory()) return true;
        if (!folder.exists() && folder.mkdirs()) return true;
        String folderAuxName = folder.getAbsolutePath();
        boolean renamed = false;
        File folderAux;
        try {
            do {
                folderAuxName = folderAuxName.substring(0, folderAuxName.lastIndexOf("/"));
                folderAux = new File(folderAuxName);
                if (folderAux.exists() && folderAux.isFile()) {
                    int j = 0;
                    do {
                        renamed = folderAux.renameTo(new File(folderAuxName + ".igual"));
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                        j++;
                    } while (!renamed && j < 10);
                    if (!renamed) {
                        SimpleLog.getInstance().writeLog(3, "Erro na renomeação do arquivo " + folderAuxName);
                        return false;
                    }
                    folderAux = new File(folderAuxName);
                    if (!folderAux.mkdirs()) {
                        SimpleLog.getInstance().writeLog(3, "Erro na geração da estrutura de" + " pastas em " + folderAuxName);
                        return false;
                    }
                }
            } while (!renamed && folderAuxName.lastIndexOf("/") > 0);
            return ((folder.exists() && folder.isDirectory() || folder.mkdirs()));
        } catch (StringIndexOutOfBoundsException e) {
            SimpleLog.getInstance().writeLog(3, "Erro na formação da URL: " + fileName);
            SimpleLog.getInstance().writeException(e, 3);
            return false;
        }
    }

    public static String makeFilePath(String address, String hostPath, String rootFolder, long id) {
        String filePath = "";
        filePath = hostPath.trim();
        if (!filePath.endsWith("/")) {
            filePath += "/";
        }
        if (address.contains("/")) {
            filePath += address.substring(address.indexOf('/') + 1);
        }
        String query = null;
        if (address.contains("?")) {
            filePath = filePath.substring(0, filePath.indexOf('?'));
            query = address.substring(address.indexOf('?') + 1);
        }
        for (int i = 0; i < Constants.DEFAULT_ENCONDING.length; i++) {
            if (!Constants.DEFAULT_ENCONDING[i][0].equals("/")) filePath = filePath.replaceAll(Constants.DEFAULT_ENCONDING[i][0], Constants.DEFAULT_ENCONDING[i][1]);
            if (query != null) query = query.replaceAll(Constants.DEFAULT_ENCONDING[i][0], Constants.DEFAULT_ENCONDING[i][1]);
        }
        if (query != null) filePath += "?" + query;
        filePath = filePath.trim();
        if (filePath.endsWith("/")) {
            filePath += "index.r00t";
        }
        if (filePath.contains("/") && filePath.length() - filePath.lastIndexOf("/") > 251) {
            SimpleLog.getInstance().writeLog("[renamed]" + id + ", " + filePath.substring(filePath.lastIndexOf("/") + 1) + "\n", Constants.STATIC_ROOT_FOLDER + "/renamedFiles.csv");
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1) + "[renamed]" + id;
        }
        return filePath;
    }

    public static String matchTLD(String dominio, List<String> tlds) {
        for (Iterator<String> it = tlds.iterator(); it.hasNext(); ) {
            String next = it.next();
            if (dominio.endsWith(next)) return next;
        }
        return ".int";
    }

    /**
	 * Executa o parsing de uma linha de CSV fornecida, substituindo instâncias
	 * de [comma] por vírgulas, não utilizando-as para separar os valores neste
	 * caso.
	 */
    public static List<String> parseCSVLine(String line, String fieldSeparator) {
        List<String> parsedLine = new ArrayList<String>();
        String[] separatedValues = line.split(fieldSeparator);
        for (int i = 0; i < separatedValues.length; i++) {
            String parsedVal = separatedValues[i];
            parsedVal = parsedVal.replaceAll("\\[comma\\]", ",");
            parsedLine.add(parsedVal);
        }
        return parsedLine;
    }

    /**
	 * Realiza o parsing da lista de hosts indicada
	 * 
	 * @param filename
	 *   Arquivo contendo a Lista de hosts a ser parseada, sempre no
	 *   formato "http://[host]/<Qualquer outra coisa>", sem nada antes do http://.
	 * @return Uma lista contendo todos os hosts identificados no arquivo, ou
	 *   null caso não se tenha conseguido acessar o arquivo fornecido.
	 */
    public static List<String> parseHostsList(String filename) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
            List<String> hostsList = new ArrayList<String>();
            boolean isCsvSitio = false;
            if (br.ready()) {
                String firstLine = br.readLine();
                int numFields = firstLine.split(",").length;
                if (numFields == Constants.CSVHOST_NUMBER_OF_FIELDS) isCsvSitio = true;
                if (!isCsvSitio) {
                    hostsList.add(getPageHost(firstLine.trim()));
                }
            }
            while (br.ready()) {
                String line = br.readLine().trim();
                String host = null;
                if (isCsvSitio) {
                    String[] fields = line.split(",");
                    if (fields[29] != null) host = getPageHost(fields[29].trim());
                } else {
                    host = getPageHost(line);
                }
                if (host != null && !host.isEmpty()) {
                    hostsList.add(host);
                }
            }
            Collections.shuffle(hostsList);
            return hostsList;
        } catch (IOException e) {
            SimpleLog.getInstance().writeLog(4, "Erro no parsing do arquivo de hosts em " + filename + ".");
            return null;
        } finally {
            if (br != null) try {
                br.close();
            } catch (IOException e) {
            }
        }
    }

    public static int parseHttpStatus(InputStream rd) throws NumberFormatException, IOException {
        int http_status = Constants.HTTP_STATUS_UNDEFINED;
        boolean readed_http_code = false;
        String line = "";
        char c;
        while (!((c = (char) rd.read()) == -1)) {
            if (c == '\n') {
                if (line == "") break;
                if (readed_http_code) {
                } else {
                    line = line.trim();
                    String[] splitedLine = line.split(" ");
                    if (line.length() < 9 || splitedLine.length < 2) {
                        http_status = Constants.HTTP_ERROR_PROTOCOL;
                    } else {
                        try {
                            http_status = Integer.parseInt(splitedLine[1]);
                        } catch (NumberFormatException nfe) {
                            http_status = 0;
                        }
                        if (http_status < 100) {
                            http_status = Constants.HTTP_ERROR_PROTOCOL;
                        }
                    }
                    if (http_status == Constants.HTTP_ERROR_PROTOCOL) {
                        SimpleLog.getInstance().writeLog(3, "No http code received in this line " + line);
                    } else {
                        readed_http_code = true;
                    }
                }
                line = "";
            } else if (c == '\r') {
            } else {
                line += c;
            }
        }
        if (http_status == Constants.HTTP_STATUS_UNDEFINED) {
            http_status = Constants.HTTP_ERROR_PROTOCOL;
        } else if (Utils.httpResponseSuccessful(http_status) && !"".equals(line)) {
            http_status = Constants.HTTP_ERROR_PROTOCOL;
        }
        return http_status;
    }

    /**
	 * Prints a short help, with only the commands. Indeed, prints only the header.
	 */
    public static void printShortHelp() {
        System.out.println(Utils.helpHeader());
        System.out.println("***********************************");
        System.out.println("Coloque --help como primeira opção para maiores detalhes.");
    }

    /**
	 * Returns the header part of the help, that should be printed as needed.
	 * @return
	 */
    public static String helpHeader() {
        String header = "Formato: [" + Constants.INPUT_TYPE_RETEST + "] [" + Constants.INPUT_TYPE_MAXTHREADS + " <Quantidade de threads a serem usadas>] [" + Constants.INPUT_TYPE_ENCRYPT + "] [" + Constants.INPUT_TYPE_DATABASE + " <url do banco de dados e.g. //localhost:3306/teste>] [" + Constants.INPUT_TYPE_DATABASE_USER + " <login do banco de dados>] [" + Constants.INPUT_TYPE_DATABASE_PASSWORD + " <senha do usuário banco de dados>] [" + Constants.LINKS_DONT_EXCLUDE_SITES + "] [" + Constants.INPUT_TYPE_CONSOLIDATE + " <arquivo + extensão para onde se irá exportar>] {" + Constants.INPUT_TYPE_CONSOLIDATEBR + " <arquivo + extensão para onde se irá exportar>] {";
        for (int i = 0; i < Constants.INPUT_TYPES.length; i++) {
            header += Constants.INPUT_TYPES[i] + " , ";
        }
        header += ") <local do arquivo/diretório alvo>";
        return header;
    }

    /**
	 * Prints a longer version of the help, with an explanation of how each function works.
	 */
    public static void printHelp() {
        System.out.println(Utils.helpHeader());
        System.out.println("\nDETALHES DAS OPÇÕES\n");
        System.out.println(Constants.INPUT_TYPE_RETEST + ": Indica se, caso possível irão ser " + "utilizados os dados já obtidos previamente para o teste indicado.\n");
        System.out.println(Constants.INPUT_TYPE_MAXTHREADS + ": Indica a quantidade de threads a serem inicializadas para" + " execução do programa. Deve ser seguida da quantidade" + " desejada.\n");
        System.out.println(Constants.INPUT_TYPE_ENCRYPT + ": Indica se resultados do teste " + "que possa identificar unicamente os locais testados serão salvos ou não. Se " + "presente, todos os dados e tal situação serão salvos usando uma função de " + "Hash " + Constants.DEFAULT_HASHFUNCTION + ".\n");
        System.out.println(Constants.LINKS_DONT_EXCLUDE_SITES + ": Argumento válido apenas para o teste de linksCompleto. " + "indica se site com grandes quantidades de links inacessíveis devem ser" + " excluidos do processo de análise de requisições HTTP HEAD");
        System.out.println(Constants.INPUT_TYPE_CONSOLIDATE + ": Realiza a consolidação dos dados do banco de dados para certos parâmetros" + " estatísticos. Seguido do local onde os csvs da consolidação serão escritos.\n");
        System.out.println(Constants.INPUT_TYPE_CONSOLIDATEBR + ": Realiza a consolidação dos dados do banco de dados para certos parâmetros" + " estatísticos, considerando TLDs apenas no .br. Seguido do local onde os csvs" + " da consolidação serão escritos.\n");
        for (int i = 0; i < Constants.INPUT_DETAILS.length; i++) {
            System.out.println(Constants.INPUT_TYPES[i] + ": " + Constants.INPUT_DETAILS[i] + "\nEntrada: " + Constants.INPUT_PARAMETER[i] + "\n");
        }
    }

    /**
	 * Removes, if present, the http:// from an address.
	 */
    public static final String removeHttp(String address) {
        if (address.startsWith("http://")) address = address.substring(7);
        return address;
    }

    /**
	 * Cria um novo path, trocando seu nome para um no formato "[renamed]id".
	 * @param hostPath
	 * Nome do arquivo original
	 * @param id
	 * Id que será colocado após o renamed. Sempre que possível, deve ser o WireId da
	 * página correspondente.
	 * @return
	 * Retorna o arquivo que foi renomeado, quando a renomeação teve sucesso.
	 * @throws IOException
	 * Caso o arquivo ainda não exista, e seus diretórios não possam ser criados.
	 */
    private static File renamePath(String hostPath, long id) throws IOException {
        String filePath = hostPath.trim();
        if (!filePath.endsWith("/")) {
            filePath += "/";
        }
        filePath += "[renamed]" + id;
        File savedFile = new File(filePath);
        if (!savedFile.createNewFile()) {
            if (!makeDirs(savedFile)) throw new IOException("Erro! impossível criar o path: " + filePath);
        }
        return savedFile;
    }

    /**
	 * Preenche o mapa contendo a relação entre nome dos arquivos renomeados, e
	 * o nome do arquivo original, a partir de um arquivo definido a partir da
	 * pasta raíz em Constants.DEFAULT_RENAMEDARCHIVE. O Mapa é salvo,
	 * permitindo seu acesso por todo o programa, em
	 * Constants.STATICS_RENAMED_LIST.
	 * 
	 * @param rootPath
	 *   Diretório raiz, a partir do qual se localiza o arquivo com os nomes.
	 */
    public static void startRenamedList(String rootPath) {
        Constants.STATICS_RENAMED_LIST = new HashMap<String, String>();
        if (!rootPath.endsWith("/")) rootPath += "/";
        File renamedListFile = new File(rootPath + Constants.DEFAULT_RENAMEDARCHIVE);
        String line = null;
        if (renamedListFile.exists() && renamedListFile.canRead()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(renamedListFile));
                while (reader.ready()) {
                    line = reader.readLine();
                    if (!line.trim().equals("")) {
                        String[] split = line.split(",", 2);
                        if (split.length == 2) Constants.STATICS_RENAMED_LIST.put(split[0].trim(), split[1].trim());
                    }
                }
            } catch (Exception e) {
                System.out.println("erro em " + line);
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        SimpleLog.getInstance().writeException(e, 7);
                    }
                }
            }
        } else {
            System.out.println("não achei o arquivo em " + rootPath + Constants.DEFAULT_RENAMEDARCHIVE + "!");
        }
    }

    /**
	 * Troca o "www." no início de "host" por "substitute". Caso o host não
	 * comece com www., simplesmente anexa substitue ao início de host.
	 * Caso substitute seja vazio, simplesmente remove o www.
	 */
    public static String substituteWWW(String host, String substitute) {
        boolean http = false;
        if (host.startsWith("www.")) {
            host = host.substring(4);
        } else if (host.startsWith("http://www.")) {
            host = host.substring(11);
            http = true;
        } else if (host.startsWith("http://")) {
            http = true;
            host = host.substring(7);
        }
        host = (substitute.equals("") ? "" : substitute + ".") + host;
        if (http) host = "http://" + host;
        return host;
    }
}
