package projetofinal.controle;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import projetofinal.modelo.Avaliacao;
import projetofinal.modelo.AvaliacaoLugar;
import projetofinal.modelo.AvaliacaoUnidade;
import projetofinal.modelo.Comentario;
import projetofinal.modelo.Dicionario;
import projetofinal.modelo.Lugar;
import projetofinal.modelo.Unidade;
import projetofinal.modelo.Usuario;

public class Login {

    public static boolean estaLogado = false;

    public static long idUsuario = 0;

    public static boolean flag_enviar_comentarios_servidor = false;

    public static boolean flag_enviar_comentarios_atualizados_servidor = false;

    public static boolean flag_enviar_unidades_atualizadas_servidor = false;

    public static boolean flag_enviar_lugares_atualizados_servidor = false;

    public static boolean flag_enviar_usuarios_atualizados_servidor = false;

    public static boolean flag_enviar_avaliacoes_servidor = false;

    public static boolean flag_enviar_avaliacoes_atualizados_servidor = false;

    public static boolean flag_enviar_avaliacoesUnidade_servidor = false;

    public static boolean flag_enviar_avaliacoesUnidade_atualizadas_servidor = false;

    public static boolean flag_enviar_avaliacoesLugar_servidor = false;

    public static boolean flag_enviar_avaliacoesLugar_atualizadas_servidor = false;

    public static boolean flag_enviar_dicionarios_servidor = false;

    public static ArrayList<Usuario> usuarios_atualizados_locais = new ArrayList<Usuario>();

    public static ArrayList<Comentario> comentarios_locais = new ArrayList<Comentario>();

    public static ArrayList<Comentario> comentarios_atualizados_locais = new ArrayList<Comentario>();

    public static ArrayList<Unidade> unidades_atualizadas_locais = new ArrayList<Unidade>();

    public static ArrayList<Lugar> lugares_atualizados_locais = new ArrayList<Lugar>();

    public static ArrayList<Avaliacao> avaliacoes_locais = new ArrayList<Avaliacao>();

    public static ArrayList<Avaliacao> avaliacoes_atualizados_locais = new ArrayList<Avaliacao>();

    public static ArrayList<AvaliacaoUnidade> avaliacoesUnidade_locais = new ArrayList<AvaliacaoUnidade>();

    public static ArrayList<AvaliacaoUnidade> avaliacoesUnidade_atualizadas_locais = new ArrayList<AvaliacaoUnidade>();

    public static ArrayList<AvaliacaoLugar> avaliacoesLugar_locais = new ArrayList<AvaliacaoLugar>();

    public static ArrayList<AvaliacaoLugar> avaliacoesLugar_atualizadas_locais = new ArrayList<AvaliacaoLugar>();

    public static ArrayList<Dicionario> dicionarios_locais = new ArrayList<Dicionario>();

    public static long idCentroAtual = 0;

    public static String hashMD5(String password) {
        String hashword = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashword = hash.toString(16);
        } catch (NoSuchAlgorithmException nsae) {
        }
        return hashword;
    }
}
