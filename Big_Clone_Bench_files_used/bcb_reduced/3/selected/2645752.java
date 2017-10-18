package util;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Wagner Borges
 */
public class FacesUtil {

    /**
     * return o objeto que está na sessão com o nome
     * fornececido no paramentro attributeName
     * @author Wagner Borges
     * @param attributeName
     * @return
     */
    public static Object getSessionAttribute(String attributeName) {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(true);
        Object o = session.getAttribute(attributeName);
        return o;
    }

    /**
     * seta um objeto na sessão. attributeName é o nome do objeto
     * na sessão. value é o proprio objeto.
     * @author Wagner Borges
     * @param attributeName
     * @param value
     */
    public static void setSessionAttribute(String attributeName, Object value) {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(false);
        session.setAttribute(attributeName, value);
    }

    public static void removeSessionAttribute(String attributeName) {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(false);
        session.removeAttribute(attributeName);
    }

    public static void sessionInvalidate() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(false);
        session.invalidate();
    }

    /**
     * retorna o objeto HttpServletReuqest atual. ou seja, o objeto
     * que representa a requisição do cliente.
     * @return
     */
    public static HttpServletRequest request() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        return request;
    }

    /**
     * retorna o valor de um determinado paramentro de requisição
     * @param name
     * @return
     */
    public static String getParamenter(String name) {
        return request().getParameter(name);
    }

    /**
     * retorna o objeto HttpSerletResponse atual
     * @return
     */
    public static HttpServletResponse response() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
        return response;
    }

    /**
     * Redirenciona o fluxo para o caminho especificado
     * pelo paramentro path
     * @param path
     */
    public static void redirectTo(String path) {
        try {
            if (path.substring(0, 1).equals("/")) {
                response().sendRedirect(request().getContextPath() + path);
            } else {
                response().sendRedirect(request().getContextPath() + "/" + path);
            }
        } catch (IOException ex) {
            Logger.getLogger(FacesUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Esse método facilita as validações no jsp.
     * Quando precisar validar algum campo cri um método
     * no seu bean manager com a seguinte assinatura:
     *
     * <b>public void nomeMetodo(FacesContext context, UIComponent toValidate, Object value)</b>
     *
     * No seu componente jsf use assim: <h:inputText validator="nomeMetodo"/>
     *
     * O objeto <b>value</b> possui o valor digitado em seu componente.
     *
     * faça as validações usando o objeto value, exemplo:
     *
     * if(value xx XX){
     *     FacesUtils.showGenericMessage("Valor invalido para o campo xxx");
     * }
     *
     * esta mensagem vai aparecer no seu <h:message.../>
     * 
     * @param mensagem
     */
    public static void showGenericMessage(String mensagem) {
        FacesMessage message = new FacesMessage(mensagem);
        message.setSeverity(FacesMessage.SEVERITY_ERROR);
        throw new ValidatorException(message);
    }

    /**
     * Esta mensagem aparecera no <h:messages ou no <rich:messages>
     * @param mensagem Mensagem de texto que deve aparecer
     * @param nivel_severidade pode ser:
     *  FacesMessage.SEVERITY_ERROR;
    FacesMessage.SEVERITY_FATAL;
    FacesMessage.SEVERITY_INFO;
    FacesMessage.SEVERITY_WARN;
     */
    @SuppressWarnings("static-access")
    public static void showGenericMessage(String mensagem, Severity nivel_severidade) {
        FacesMessage message = new FacesMessage(mensagem);
        message.setSeverity(nivel_severidade);
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.getCurrentInstance().addMessage(null, message);
    }

    /**
     * Esta mensagem aparecera no <h:messages ou no <rich:messages>
     * @param mensagem Mensagem de texto que deve aparecer
     * @param nivel_severidade pode ser:
     *  FacesMessage.SEVERITY_ERROR;
     *  FacesMessage.SEVERITY_FATAL;
     *  FacesMessage.SEVERITY_INFO;
     *  FacesMessage.SEVERITY_WARN;
     * @param detalhe Detalhe da mensagem para ser exibido.
     */
    @SuppressWarnings("static-access")
    public static void showGenericMessage(String mensagem, Severity nivel_severidade, String detalhe) {
        FacesMessage message = new FacesMessage(mensagem);
        message.setSeverity(nivel_severidade);
        message.setDetail(detalhe);
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.getCurrentInstance().addMessage(null, message);
    }

    /**
     * Esta mensagem aparecera no <h:messages ou no <rich:messages>
     * @param mensagem Mensagem de texto que deve aparecer
     * @param detalhe Detalhe da mensagem para ser exibido.
     */
    @SuppressWarnings("static-access")
    public static void showGenericMessage(String mensagem, String detalhe) {
        FacesMessage message = new FacesMessage(mensagem);
        message.setSeverity(FacesMessage.SEVERITY_ERROR);
        message.setDetail(detalhe);
        throw new ValidatorException(message);
    }

    /**
     * método responsavel por preencher um combobox de forma generica.
     * o atributo lista representa a uma coleção de objetos de uma entidade qualquer.
     * o método toString() deve ser sobrescrito para mostrar o que se deseja no combo.
     * os itens do combo são prenchido do tipo <valor, nome> onde <b>nome</b>
     * representa o valor que está sendo exibido e <b>valor</b> uma referencia
     * para um objeto da entidade.
     * @param lista
     * @return
     */
    public static List<SelectItem> preencheCombobox(List<? extends Object> lista) {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (Object o : lista) {
            items.add(new SelectItem(o, o.toString()));
        }
        return items;
    }

    /**
     * este métdodo retorna a instancia atual de um bean manager registrado
     * @param context
     * @param beanName
     * @return
     */
    private static Object getBean(FacesContext context, String beanName) {
        return context.getApplication().getVariableResolver().resolveVariable(context, beanName);
    }

    /**
     * retorna a instancia atual de um bean manager registrado no arquivo
     * faces-config.xml. O paramentro <b>beanName</b> representa o nome do
     * bean manager que se deseja recuperar a instancia.
     * 
     * @param beanName
     * @return
     */
    public static Object getBean(String beanName) {
        FacesContext context = FacesContext.getCurrentInstance();
        return getBean(context, beanName);
    }

    /**
     * Este método possibilita que o valor de um atributo de um determinado
     * bean manager seja alterado. O atributo <b>beanName</b> representa o
     * bean manager que possui o atributo que se deseja alterar. O paramentro
     * <b>attribute</b> reprsenta o atributo que será alterado. O paramentro
     * <b>newValue</b> representa o novo valor a ser atribuído ao atributo.
     *
     * @param beanName
     * @param attribute
     * @param value
     */
    public static void setNewValueBean(String beanName, String attribute, Object newValue) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getApplication().getELResolver().setValue(context.getELContext(), beanName, attribute, newValue);
    }

    /**
     * Retorna o valor de um  atributo de um determinado bean manager.
     * O paramentro <b>beanName</b> representa o bean manager que possui o atributo
     * que se deseja recuperar o valor. O paramentro <b>attribute</b> representa
     * o atributo que se deseja recuperar.
     * 
     * @param beanName
     * @param attribute
     * @return
     */
    public static Object getBeanValue(String beanName, String attribute) {
        FacesContext context = FacesContext.getCurrentInstance();
        Object o = context.getApplication().getELResolver().getValue(context.getELContext(), beanName, attribute);
        return o;
    }

    public static String getHostRequest() {
        return request().getRemoteHost();
    }

    public static boolean validaCPF(String cpf) {
        int digito;
        boolean resp = true;
        cpf = preencheStringComCaracter(cpf, '0', (byte) 11);
        for (int j = 1; j < 3; j++) {
            digito = 0;
            for (int i = 0; i < 8 + j; i++) {
                digito = Integer.parseInt("" + cpf.charAt(i)) * ((10 + j) - (i + 1)) + digito;
            }
            digito = digito % 11;
            if (digito < 2) {
                digito = 0;
            } else {
                digito = 11 - digito;
            }
            if (Integer.parseInt("" + cpf.charAt(8 + j)) != digito) {
                resp = false;
            }
        }
        return resp;
    }

    public static boolean validaCNPJ(String str_cnpj) {
        @SuppressWarnings("unused") int soma = 0, aux, dig;
        String cnpj_calc = str_cnpj.substring(0, 12);
        if (str_cnpj.length() != 14) {
            return false;
        }
        char[] chr_cnpj = str_cnpj.toCharArray();
        for (int i = 0; i < 4; i++) {
            if (chr_cnpj[i] - 48 >= 0 && chr_cnpj[i] - 48 <= 9) {
                soma += (chr_cnpj[i] - 48) * (6 - (i + 1));
            }
        }
        for (int i = 0; i < 8; i++) {
            if (chr_cnpj[i + 4] - 48 >= 0 && chr_cnpj[i + 4] - 48 <= 9) {
                soma += (chr_cnpj[i + 4] - 48) * (10 - (i + 1));
            }
        }
        dig = 11 - (soma % 11);
        cnpj_calc += (dig == 10 || dig == 11) ? "0" : Integer.toString(dig);
        soma = 0;
        for (int i = 0; i < 5; i++) {
            if (chr_cnpj[i] - 48 >= 0 && chr_cnpj[i] - 48 <= 9) {
                soma += (chr_cnpj[i] - 48) * (7 - (i + 1));
            }
        }
        for (int i = 0; i < 8; i++) {
            if (chr_cnpj[i + 5] - 48 >= 0 && chr_cnpj[i + 5] - 48 <= 9) {
                soma += (chr_cnpj[i + 5] - 48) * (10 - (i + 1));
            }
        }
        dig = 11 - (soma % 11);
        cnpj_calc += (dig == 10 || dig == 11) ? "0" : Integer.toString(dig);
        return str_cnpj.equals(cnpj_calc);
    }

    public static String gerarMatriculaAdesaoPlano(String regPlano, String quant) {
        String mat2 = "", mat3 = "";
        for (int i = 0; i < 6; i++) {
            if (5 - regPlano.length() >= i) {
                mat2 += "0";
            }
            if (5 - quant.length() >= i) {
                mat3 += "0";
            }
        }
        regPlano = (mat2 + regPlano) + (mat3 + quant);
        regPlano = geraDigitoVerificadorModulo11(regPlano, 13);
        return regPlano;
    }

    public static String geraDigitoVerificadorModulo11(String codigo, int tamanho) {
        if (codigo.length() + 1 == tamanho) {
            int soma = 0;
            String fator = "876543298765432";
            for (int i = codigo.length() - 1; i >= 0; i--) {
                soma += Integer.parseInt(codigo.charAt(i) + "") * Integer.parseInt(fator.charAt(i) + "");
            }
            int resto = 11 - (soma % 11);
            if (resto == 10 || resto == 11) {
                resto = 0;
            }
            String dVerificador = "" + resto;
            codigo += dVerificador;
        }
        return codigo;
    }

    public static String preencheStringComCaracter(String texto, char caracter, byte tamanho) {
        String textoRetorno = "";
        byte contador = 0;
        if (texto.length() < tamanho) {
            for (contador = 1; contador <= (tamanho - texto.length()); contador++) {
                textoRetorno += caracter;
            }
            return (textoRetorno + texto);
        } else {
            return texto;
        }
    }

    public static String textoAlfanumericoAleatorio(int qtdeLetras, int qtdeNumeros) {
        int totalCaracteres = qtdeLetras + qtdeNumeros;
        Random random = new Random();
        StringBuilder sb = new StringBuilder(totalCaracteres);
        for (int i = 0; i < totalCaracteres; i++) {
            if (i < qtdeLetras) {
                sb.append((char) (random.nextInt(26) + 65));
            } else {
                sb.append((int) (random.nextInt(10)));
            }
        }
        return sb.toString();
    }

    public static String semDelimitadores(String str) {
        if (str == null) {
            return "";
        }
        return str.replaceAll("[^\\d*]", "");
    }

    public static String cnpjComDelimitadores(String cnpj) {
        String retorno = semDelimitadores(cnpj);
        while (retorno.length() < 14) {
            retorno = "0" + retorno;
        }
        return retorno.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    public static String cpfComDelimitadores(String cpf) {
        String retorno = semDelimitadores(cpf);
        while (retorno.length() < 11) {
            retorno = "0" + retorno;
        }
        return retorno.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    public static String cepComDelimitadores(String cep) {
        if (cep != null) {
            return cep.replaceAll("(\\d{5})(\\d{3})", "$1-$2");
        }
        return "";
    }

    public static String foneComDelimitadores(String fone) {
        if (fone != null) {
            return fone.replaceAll("(\\d{4})(\\d{4})", "$1-$2");
        }
        return "";
    }

    public static String dateToStr(Date data) {
        if (data == null) {
            return "";
        }
        SimpleDateFormat dt = new SimpleDateFormat("dd/MM/yyyy");
        return dt.format(data);
    }

    public static String horaAtual() {
        Date data = new Date();
        SimpleDateFormat dt = new SimpleDateFormat("HH:mm");
        return dt.format(data);
    }

    public static int recuperaIdade(Date dtNascimento) {
        Calendar dateOfBirth = new GregorianCalendar();
        dateOfBirth.setTime(dtNascimento);
        Calendar today = Calendar.getInstance();
        int idade = today.get(Calendar.YEAR) - dateOfBirth.get(Calendar.YEAR);
        dateOfBirth.add(Calendar.YEAR, idade);
        if (today.before(dateOfBirth)) {
            idade--;
        }
        return idade;
    }

    public static long recuperaPeriodoDeUtilizacaoPlano(Date dtAdesao) {
        Calendar dataInicio = Calendar.getInstance();
        dataInicio.setTime(dtAdesao);
        Calendar dataFinal = Calendar.getInstance();
        long diferenca = dataFinal.getTimeInMillis() - dataInicio.getTimeInMillis();
        int tempoDia = 1000 * 60 * 60 * 24;
        long diasDiferenca = diferenca / tempoDia;
        return diasDiferenca;
    }

    public static Date strToDate(String str) throws Exception {
        SimpleDateFormat form = new SimpleDateFormat("dd/MM/yyyy");
        try {
            return form.parse(str);
        } catch (Exception e) {
            throw new Exception("Data inválida. '" + str + "'");
        }
    }

    public static String getDiretorioReal(String diretorio) {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        return session.getServletContext().getRealPath(diretorio);
    }

    public static Date subtraiData(int i) {
        Calendar data = Calendar.getInstance();
        data.add(Calendar.DATE, -i);
        return data.getTime();
    }

    public static Date subtraiMesesDaData(Date data, int qtMesSubtraido) {
        Calendar dt = new GregorianCalendar();
        dt.setTime(data);
        dt.add(Calendar.MONTH, -qtMesSubtraido);
        return dt.getTime();
    }

    public static String criptografaSenha(String senha) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        BigInteger hash = new BigInteger(1, md.digest(senha.getBytes()));
        String s = hash.toString(16);
        if (s.length() % 2 != 0) {
            s = "0" + s;
        }
        return s;
    }

    public static void gerarDownloadPdf(byte[] saida) throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse resp = (HttpServletResponse) fc.getExternalContext().getResponse();
        resp.addHeader("Content-Disposition", "attachment; filename=" + "file_Magister" + ".pdf");
        resp.setContentType("application/pdf");
        ServletOutputStream out = resp.getOutputStream();
        out.write(saida);
        out.flush();
        out.close();
        fc.responseComplete();
    }
}
