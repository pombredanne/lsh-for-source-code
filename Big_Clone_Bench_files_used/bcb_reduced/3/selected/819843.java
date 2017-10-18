package recursos;

import java.sql.*;
import java.util.Vector;

public class TISS {

    private Connection con = null;

    private Statement stmt = null;

    private String[] vetorCD = { "Gases Medicinais", "Medicamentos", "Materiais", "Taxas Diversas", "Di�rias", "Alugu�is" };

    private Vector anteriores;

    public TISS() {
        con = Conecta.getInstance();
    }

    public String gerarGuia(String cod_fatura, String cod_empresa, String usuario) {
        String sql = "", resp = "";
        ResultSet rs = null;
        sql += "SELECT faturas.Numero, faturas_itens.cod_convenio, procedimentos.tipoGuia, ";
        sql += "convenio.descr_convenio FROM ((faturas_itens INNER JOIN procedimentos ";
        sql += "ON faturas_itens.Cod_Proced = procedimentos.COD_PROCED) INNER JOIN faturas ";
        sql += "ON faturas_itens.Numero = faturas.Numero) INNER JOIN convenio ";
        sql += "ON faturas_itens.cod_convenio = convenio.cod_convenio ";
        sql += "WHERE faturas.Numero=" + cod_fatura + " ";
        sql += "GROUP BY faturas.Numero, faturas_itens.cod_convenio, procedimentos.tipoGuia, ";
        sql += "convenio.descr_convenio ORDER BY procedimentos.tipoGuia";
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            carregaGuiasAnteriores(cod_fatura);
            apagarGuia(cod_fatura, cod_empresa);
            while (rs.next()) {
                if (!rs.getString("descr_convenio").equalsIgnoreCase("particular") && !rs.getString("cod_convenio").equals("-1")) {
                    if (!Util.isNull(rs.getString("tipoGuia"))) {
                        if (rs.getString("tipoGuia").equals("1")) resp = gerarGuiaConsulta(cod_fatura, rs.getString("cod_convenio"), usuario); else if (rs.getString("tipoGuia").equals("2")) resp = gerarGuiaSADT(cod_fatura, rs.getString("cod_convenio"), usuario);
                    }
                }
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String getGuias(String codcli, String data, String cod_empresa, String usuario) {
        String sql = "";
        String resp = "";
        String cod_fatura = "";
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql = "SELECT Numero FROM faturas WHERE Data_Lanca='" + Util.formataDataInvertida(data);
            sql += "' AND codcli=" + codcli;
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                cod_fatura = rs.getString("Numero");
                resp += getGuias(cod_fatura, cod_empresa, usuario);
            }
        } catch (SQLException e) {
            resp = "Erro: " + e.toString() + "Sql: " + sql;
        }
        return resp;
    }

    public String existeOutrasDespesas(String codGuia, String cod_empresa) {
        String sql = "";
        String achou = "N";
        try {
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql = "SELECT * FROM guiasoutrasdespesas WHERE codGuia=" + codGuia;
            sql += " AND cod_empresa=" + cod_empresa;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                achou = "S";
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            achou = "ERRO: " + e.toString() + " SQL: " + sql;
        }
        return achou;
    }

    public String getItensOutrasDespesas(String codGuia, String cod_empresa) {
        String sql = "";
        String resp = "";
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            sql = "SELECT * FROM guiasoutrasdespesas WHERE codGuia=" + codGuia + " AND cod_empresa=" + cod_empresa;
            ResultSet rs = stmt.executeQuery(sql);
            resp = "<tr>\n";
            resp += " <td class='dados'><b>6-CD</b></td>\n";
            resp += " <td class='dados'><b>7-Data</b></td>\n";
            resp += " <td class='dados'><b>8-Hora Inicial</b></td>\n";
            resp += " <td class='dados'><b>9-Hora Final</b></td>\n";
            resp += " <td class='dados'><b>10-Tabela</b></td>\n";
            resp += " <td class='dados'><b>11-C�digo do Item</b></td>\n";
            resp += " <td class='dados'><b>12-Descri��o</b></td>\n";
            resp += " <td class='dados'><b>13-Qtde.</b></td>\n";
            resp += " <td class='dados'><b>14-% Red/Acresc.</b></td>\n";
            resp += " <td class='dados'><b>15-Valor Unit�rio R$</b></td>\n";
            resp += " <td class='dados'><b>16-Valor Total R$</b></td>\n";
            resp += "</tr>\n";
            int cont = 1;
            while (rs.next()) {
                resp += "<tr>\n";
                resp += " <td class='dados'>" + cont + "-" + rs.getString("tipoDespesa") + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + Util.formataData(rs.getString("dataRealizacao")) + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + Util.formataHora(rs.getString("horaInicial")) + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + Util.formataHora(rs.getString("horaFinal")) + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + rs.getString("tipoTabela") + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + rs.getString("codigo") + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + rs.getString("descricao") + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + rs.getString("quantidade") + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + Util.trataNulo(rs.getString("reducaoAcrescimo"), "") + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + Util.formatCurrency(rs.getString("valorUnitario")) + "&nbsp;</td>\n";
                resp += " <td class='dados'>" + Util.formatCurrency(rs.getString("valorTotal")) + "&nbsp;</td>\n";
                resp += "</tr>\n";
                resp += "<tr><td colspan='11' height='12px'></td></tr>\n";
                cont++;
            }
            for (int i = cont; i <= 20; i++) {
                resp += "<tr>\n";
                resp += " <td class='dados'>" + i + "-&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += " <td class='dados'>&nbsp;</td>\n";
                resp += "</tr>\n";
                resp += "<tr><td colspan='11' height='12px'></td></tr>\n";
            }
        } catch (SQLException e) {
            resp = "Erro: " + e.toString() + "Sql: " + sql;
        }
        return resp;
    }

    public String[] getDadosGuia(String codGuia, String cod_empresa) {
        String sql = "";
        String resp[] = { "", "", "", "", "", "", "", "", "", "", "" };
        ResultSet rs = null;
        if (Util.isNull(codGuia)) return resp;
        try {
            sql = "SELECT registroANS, numeroGuiaPrestador, identificacaoContratado, ";
            sql += "contratado, codCNES, gases, medicamentos, materiais, taxas, diarias ";
            sql += "FROM guiassadt WHERE codGuia=" + codGuia;
            sql += " AND cod_empresa=" + cod_empresa;
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            float soma = 0;
            if (rs.next()) {
                resp[0] = rs.getString("registroANS");
                resp[1] = rs.getString("numeroGuiaPrestador");
                resp[2] = rs.getString("identificacaoContratado");
                resp[3] = rs.getString("contratado");
                resp[4] = rs.getString("codCNES");
                resp[5] = rs.getString("gases");
                resp[6] = rs.getString("medicamentos");
                resp[7] = rs.getString("materiais");
                resp[8] = rs.getString("taxas");
                resp[9] = rs.getString("diarias");
                for (int i = 5; i <= 9; i++) {
                    soma += Float.parseFloat(resp[i]);
                }
                resp[10] = soma + "";
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            resp[0] = "ERRO: " + e.toString() + "SQL: " + sql;
        }
        return resp;
    }

    public String getGuias(String cod_fatura, String cod_empresa, String usuario) {
        String sql = "";
        String resp = "";
        ResultSet rs = null;
        String outrasdespesas = "";
        if (Util.isNull(cod_fatura)) return "";
        try {
            gerarGuia(cod_fatura, cod_empresa, usuario);
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            resp += "<table cellspacing=0 cellpadding=0 width='100%'>";
            sql = "SELECT codGuia, numeroGuiaPrestador FROM guiasconsulta WHERE numeroFatura=" + cod_fatura;
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                resp += " <tr>";
                resp += "  <td class='tdMedium'><a href='frmguiaconsulta.jsp?cod=" + rs.getString("codGuia") + "'>Guia de Consulta n� " + Util.formataNumero(rs.getString("numeroGuiaPrestador"), 12) + "</a></td>";
                resp += " </tr>";
            }
            sql = "SELECT codGuia, numeroGuiaPrestador FROM guiassadt WHERE numeroFatura=" + cod_fatura;
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                outrasdespesas = existeOutrasDespesas(rs.getString("codGuia"), cod_empresa);
                resp += " <tr>";
                resp += "  <td class='tdMedium'><a href='frmguiasadt.jsp?cod=" + rs.getString("codGuia") + "&outrasdespesas=" + outrasdespesas + "'>Guia SP/SADT n� " + Util.formataNumero(rs.getString("numeroGuiaPrestador"), 12) + "</a></td>";
                resp += " </tr>";
            }
            resp += "</table>";
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            resp = "Erro: " + e.toString() + "Sql: " + sql;
        }
        return resp;
    }

    public String getCodProfissional(String prof_reg, String cod_convenio) {
        String sql = "";
        String resp = "";
        ResultSet rs = null;
        if (Util.isNull(prof_reg) || Util.isNull(cod_convenio)) return "";
        try {
            sql = "SELECT codOperadora FROM prof_convenio WHERE prof_reg='" + prof_reg;
            sql += "' AND cod_convenio=" + cod_convenio;
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                resp = rs.getString("codOperadora");
            }
        } catch (SQLException e) {
            resp = "Erro: " + e.toString() + "Sql: " + sql;
        }
        return resp;
    }

    public String gerarGuiaConsulta(String cod_fatura, String cod_convenio, String usuario) {
        String resp = "";
        String sql = "";
        Configuracao configuracao = new Configuracao();
        String proxnumeroguia = "", tipoConsulta = "", tipoSaida = "", lote = "", obs = "", usuarioCad = "";
        String codigoPrestadornaOPeradora, tipoIdentificacaoContratado, contratado, cod_empresa;
        try {
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = null;
            sql += "SELECT convenio.cod_empresa, convenio.cod_ans, paciente_convenio.num_associado_convenio, ";
            sql += "paciente.nome, planos.plano, paciente_convenio.validade_carteira, paciente.cartao_sus, ";
            sql += "CASE WHEN tipoidentificadoroperadora='1' THEN 'CNPJ' ELSE CASE WHEN tipoidentificadoroperadora='3' ";
            sql += " THEN 'cpf' ELSE 'codigoPrestadorNaOperadora' END END AS codigoPrestadornaOPeradora, ";
            sql += "convenio.identificadoroperadora, profissional.nome, ";
            sql += "profissional.reg_prof, profissional.ufConselho, profissao.tipo_registro, profissao.codCBOS, faturas.Data_Lanca, 'CID-10', ";
            sql += "valorprocedimentos.Cod_Tabela, procedimentos.CODIGO, CONCAT('Valor da Consulta: ', faturas_itens.valor), ";
            sql += "profissional.prof_reg, convenio.cod_convenio ";
            sql += "FROM valorprocedimentos INNER JOIN ((((((((faturas INNER JOIN (procedimentos INNER JOIN faturas_itens ";
            sql += "ON procedimentos.COD_PROCED = faturas_itens.Cod_Proced) ON faturas.Numero = faturas_itens.Numero) ";
            sql += "INNER JOIN convenio ON faturas_itens.cod_convenio = convenio.cod_convenio) INNER JOIN paciente ON ";
            sql += "faturas.codcli = paciente.codcli) INNER JOIN paciente_convenio ON (paciente.codcli = paciente_convenio.codcli) ";
            sql += "AND (convenio.cod_convenio = paciente_convenio.cod_convenio)) INNER JOIN planos ON ";
            sql += "(faturas_itens.cod_plano = planos.cod_plano) AND (convenio.cod_convenio = planos.cod_convenio)) ";
            sql += "INNER JOIN profissional ON faturas.prof_reg = profissional.prof_reg) INNER JOIN prof_esp ON ";
            sql += "profissional.prof_reg = prof_esp.prof_reg) INNER JOIN (profissao INNER JOIN especialidade ON ";
            sql += "profissao.cod_profis = especialidade.cod_profis) ON (prof_esp.codesp = especialidade.codesp) AND ";
            sql += "(procedimentos.codesp = especialidade.codesp)) ON (valorprocedimentos.cod_proced = faturas_itens.Cod_Proced) ";
            sql += "AND (valorprocedimentos.cod_convenio = faturas_itens.cod_convenio) AND ";
            sql += "(valorprocedimentos.cod_plano = faturas_itens.cod_plano) ";
            sql += "WHERE faturas.Numero=" + cod_fatura;
            sql += " AND faturas_itens.cod_convenio=" + cod_convenio + " AND procedimentos.tipoGuia=1";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String proxcodguia = new Banco().getNext("guiasconsulta", "codGuia");
                GuiasCadastradas atual = this.getValoresGuia(cod_convenio, "1");
                if (atual == null) {
                    proxnumeroguia = getProxGuia(cod_convenio, "1");
                    obs = rs.getString(19);
                    usuarioCad = usuario;
                } else {
                    proxnumeroguia = atual.getNumeroGuia();
                    tipoConsulta = atual.getTipoConsulta();
                    tipoSaida = atual.getTipoSaida();
                    lote = atual.getLote();
                    obs = atual.getObs();
                    usuarioCad = atual.getUsuario();
                }
                cod_empresa = rs.getString("convenio.cod_empresa");
                String pf = getCodProfissional(rs.getString("prof_reg"), rs.getString("cod_convenio"));
                if (Util.isNull(pf)) {
                    tipoIdentificacaoContratado = rs.getString(8);
                    codigoPrestadornaOPeradora = rs.getString(9);
                    contratado = configuracao.getItemConfig("nomeContratado", cod_empresa);
                } else {
                    tipoIdentificacaoContratado = "codigoPrestadorNaOperadora";
                    codigoPrestadornaOPeradora = pf;
                    contratado = rs.getString("profissional.nome");
                }
                sql = "INSERT INTO guiasconsulta ";
                sql += "(codGuia, cod_empresa, numeroFatura, registroANS, dataEmissaoGuia, numeroGuiaPrestador, numeroCarteira, ";
                sql += "nomeBeneficiario, nomePlano, validadeCarteira, numeroCNS, ";
                sql += "tipoIdentificacaoContratado, identificacaoContratado, nomeProfissional, ";
                sql += "numeroConselho, ufConselho, siglaConselho, cbos, dataAtendimento, nomeTabela,";
                sql += "codigotabela, codigoProcedimento, observacao, contratado, codCNES, ";
                sql += "tipoConsulta, tipoSaida, codLote, usuario) VALUES(" + proxcodguia + ",";
                sql += cod_empresa + ",'" + cod_fatura + "','" + rs.getString(2);
                sql += "','" + rs.getString("faturas.Data_Lanca") + "','" + proxnumeroguia;
                sql += "','" + rs.getString(3) + "','";
                sql += Util.cortaString(rs.getString(4), 70) + "','" + rs.getString(5) + "','";
                sql += rs.getString(6) + "','" + rs.getString(7) + "','";
                sql += tipoIdentificacaoContratado + "','" + codigoPrestadornaOPeradora + "','";
                sql += rs.getString(10) + "','" + rs.getString(11) + "','";
                sql += rs.getString(12) + "','" + rs.getString(13) + "','";
                sql += rs.getString(14) + "','" + rs.getString(15) + "','";
                sql += rs.getString(16) + "','" + rs.getString(17) + "','";
                sql += rs.getString(18) + "','" + obs + "','";
                sql += contratado + "','" + configuracao.getItemConfig("codCNES", cod_empresa);
                sql += "','" + tipoConsulta + "','" + tipoSaida + "','";
                sql += lote + "','" + usuarioCad + "')";
                new Banco().executaSQL(sql);
            }
            rs.close();
            stmt.close();
            return "OK";
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String gerarGuiaSADT(String cod_fatura, String cod_convenio, String usuario) {
        String sql = "";
        Configuracao configuracao = new Configuracao();
        String cod_empresa = "";
        String proxnumeroguia = "", caraterAtendimento = "", tipoAtendimento = "", tipoSaida = "", lote = "", obs = "";
        String senhaAutorizacao = "", dataAutorizacao = "NULL", validadeSenha = "NULL", usuarioCad = "", codigoDiagnostico = "";
        String codigoPrestadornaOPeradora, tipoIdentificacaoContratado, contratado;
        try {
            Statement stmt = con.createStatement();
            sql += "SELECT faturas.Numero, convenio.cod_ans, faturas_itens.cod_convenio, convenio.cod_empresa, ";
            sql += "paciente_convenio.num_associado_convenio, paciente.nome, planos.plano, ";
            sql += "CASE WHEN tipoidentificadoroperadora='1' THEN 'CNPJ' ELSE CASE WHEN tipoidentificadoroperadora='3' ";
            sql += " THEN 'cpf' ELSE 'codigoPrestadorNaOperadora' END END AS codigoPrestadornaOPeradora, ";
            sql += "paciente_convenio.validade_carteira, paciente.cartao_sus, convenio.identificadoroperadora, ";
            sql += "profissional.nome, profissao.tipo_registro, profissional.reg_prof, profissional.ufConselho, ";
            sql += "profissao.codCBOS, profissional.nome, profissao.tipo_registro, profissional.prof_reg, ";
            sql += "convenio.cod_convenio, profissao.codCBOS, faturas.Data_Lanca, faturas.hora_lanca ";
            sql += "FROM ((((paciente_convenio INNER JOIN (((faturas INNER JOIN faturas_itens ON faturas.Numero = ";
            sql += "faturas_itens.Numero) INNER JOIN convenio ON faturas_itens.cod_convenio = convenio.cod_convenio) ";
            sql += "INNER JOIN paciente ON faturas.codcli = paciente.codcli) ON (paciente_convenio.codcli = ";
            sql += "paciente.codcli) AND (paciente_convenio.cod_convenio = faturas_itens.cod_convenio)) INNER JOIN ";
            sql += "planos ON faturas_itens.cod_plano = planos.cod_plano) INNER JOIN profissional ON faturas.prof_reg = ";
            sql += "profissional.prof_reg) INNER JOIN procedimentos ON faturas_itens.Cod_Proced = procedimentos.COD_PROCED) ";
            sql += "INNER JOIN (especialidade INNER JOIN profissao ON especialidade.cod_profis = profissao.cod_profis) ";
            sql += "ON procedimentos.codesp = especialidade.codesp ";
            sql += "WHERE faturas.Numero=" + cod_fatura;
            sql += " AND faturas_itens.cod_convenio=" + cod_convenio + " AND procedimentos.tipoGuia=2";
            sql += " GROUP BY faturas.Numero, convenio.cod_ans, paciente_convenio.num_associado_convenio, ";
            sql += "paciente.nome, planos.plano, paciente_convenio.validade_carteira, paciente.cartao_sus, codigoPrestadornaOPeradora, ";
            sql += "convenio.identificadoroperadora, profissional.nome, profissao.tipo_registro, profissional.reg_prof, ";
            sql += "profissional.ufConselho, profissao.codCBOS, profissional.nome, profissao.tipo_registro, profissional.reg_prof, ";
            sql += "profissional.ufConselho, profissao.codCBOS";
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String proxcodguia = new Banco().getNext("guiassadt", "codGuia");
                GuiasCadastradas atual = this.getValoresGuia(cod_convenio, "2");
                if (atual == null) {
                    proxnumeroguia = getProxGuia(cod_convenio, "2");
                    usuarioCad = usuario;
                } else {
                    proxnumeroguia = atual.getNumeroGuia();
                    tipoAtendimento = atual.getTipoAtendimento();
                    caraterAtendimento = atual.getCaraterAtendimento();
                    tipoSaida = atual.getTipoSaida();
                    dataAutorizacao = Util.isNull(atual.getDataAutorizacao()) ? "NULL" : "'" + atual.getDataAutorizacao() + "'";
                    senhaAutorizacao = atual.getSenhaAutorizacao();
                    validadeSenha = Util.isNull(atual.getValidadeSenha()) ? "NULL" : "'" + atual.getValidadeSenha() + "'";
                    lote = atual.getLote();
                    obs = atual.getObs();
                    usuarioCad = atual.getUsuario();
                    codigoDiagnostico = atual.getCodigoDiagnostico();
                }
                cod_empresa = rs.getString("convenio.cod_empresa");
                String pf = getCodProfissional(rs.getString("prof_reg"), rs.getString("cod_convenio"));
                if (Util.isNull(pf)) {
                    tipoIdentificacaoContratado = rs.getString("codigoPrestadornaOPeradora");
                    codigoPrestadornaOPeradora = rs.getString("convenio.identificadoroperadora");
                    contratado = configuracao.getItemConfig("nomeContratado", cod_empresa);
                } else {
                    tipoIdentificacaoContratado = "codigoPrestadorNaOperadora";
                    codigoPrestadornaOPeradora = pf;
                    contratado = rs.getString("profissional.nome");
                }
                String dataProcedimento = rs.getString("faturas.Data_Lanca");
                sql = "INSERT INTO guiassadt ( dataHoraSolicitacao, cod_empresa, codGuia, numeroFatura, registroANS, dataEmissaoGuia, numeroGuiaPrestador, ";
                sql += "numeroCarteira, nomeBeneficiario, nomePlano, validadeCarteira, numeroCNS, ";
                sql += "tipoIdentificacaoContratado, identificacaoContratado, nomeProfissional, ";
                sql += "siglaConselho, numeroConselho, ufConselho, cbos, nomeExecutanteC, siglaConselhoC, ";
                sql += "numeroConselhoC, ufConselhoC, codigoCBOSC, contratado, codCNES, nomeTabela, dataHoraAtendimento, ";
                sql += "caraterAtendimento, tipoAtendimento, tipoSaida,";
                sql += "dataAutorizacao, senhaAutorizacao, validadeSenha, ContratadoSolicitante, CNESSolicitante, ";
                sql += "codLote, observacao, usuario, codigoDiagnostico) ";
                sql += "VALUES ('" + dataProcedimento;
                sql += "'," + rs.getString("convenio.cod_empresa") + "," + proxcodguia + ",";
                sql += rs.getString("faturas.Numero") + ",'" + rs.getString("convenio.cod_ans") + "','";
                sql += dataProcedimento + "','" + proxnumeroguia + "','";
                sql += rs.getString("num_associado_convenio") + "','" + Util.cortaString(rs.getString("paciente.nome"), 50) + "','";
                sql += rs.getString("planos.plano") + "','" + rs.getString("validade_carteira") + "','";
                sql += rs.getString("paciente.cartao_sus") + "','" + tipoIdentificacaoContratado + "','";
                sql += codigoPrestadornaOPeradora + "','" + rs.getString("profissional.nome");
                sql += "','" + rs.getString("profissao.tipo_registro") + "','" + rs.getString("reg_prof") + "','";
                sql += rs.getString("profissional.ufConselho") + "','" + rs.getString("profissao.codCBOS") + "','";
                sql += rs.getString("profissional.nome") + "','" + rs.getString("profissao.tipo_registro") + "','";
                sql += rs.getString("reg_prof") + "','" + rs.getString("profissional.ufConselho") + "','";
                sql += rs.getString("profissao.codCBOS") + "','" + contratado;
                sql += "','" + configuracao.getItemConfig("codCNES", cod_empresa) + "','CID-10','";
                sql += dataProcedimento + "T" + Util.formataHoraHHMMSS(rs.getString("hora_lanca"));
                sql += "','" + caraterAtendimento + "','" + tipoAtendimento + "','" + tipoSaida;
                sql += "'," + dataAutorizacao + ",'" + senhaAutorizacao + "'," + validadeSenha;
                sql += ",'" + contratado + "','" + configuracao.getItemConfig("codCNES", cod_empresa) + "','";
                sql += lote + "','" + obs + "','" + usuarioCad + "','" + codigoDiagnostico + "')";
                String cadastraSADT = new Banco().executaSQL(sql);
                if (!cadastraSADT.equals("OK")) return cadastraSADT;
                sql = "INSERT INTO procedimentossadt (codsubitem, codguia, codigo, tipoTabela, descricao, data, horaInicio, horaFim, ";
                sql += "quantidadeRealizada, valorTotal, valor, viaAcesso, cod_empresa ) ";
                sql += "SELECT faturas_itens.cod_subitem, '" + proxcodguia + "', procedimentos.CODIGO, valorprocedimentos.cod_tabela, ";
                sql += "SUBSTRING(procedimentos.Procedimento, 1, 60),";
                sql += "faturas.Data_Lanca, faturas.hora_lanca, faturas.hora_lanca, faturas_itens.qtde, ";
                sql += "(faturas_itens.valor*faturas_itens.qtde), faturas_itens.valor, faturas_itens.viaAcesso, " + cod_empresa;
                sql += " FROM valorprocedimentos INNER JOIN (faturas INNER JOIN ((procedimentos INNER JOIN faturas_itens ON ";
                sql += "procedimentos.COD_PROCED = faturas_itens.Cod_Proced) INNER JOIN convenio ON ";
                sql += "faturas_itens.cod_convenio = convenio.cod_convenio) ON faturas.Numero = faturas_itens.Numero) ON ";
                sql += "(valorprocedimentos.cod_plano = faturas_itens.cod_plano) AND ";
                sql += "(valorprocedimentos.cod_convenio = faturas_itens.cod_convenio) AND ";
                sql += "(valorprocedimentos.cod_proced = faturas_itens.Cod_Proced) ";
                sql += "WHERE faturas.Numero=" + cod_fatura + " AND faturas_itens.cod_convenio=";
                sql += cod_convenio + " AND procedimentos.tipoGuia=2";
                new Banco().executaSQL(sql);
                geraOutrasDespesas(proxcodguia, cod_convenio, cod_fatura);
                atualizaTotais(proxcodguia, cod_empresa);
            }
            stmt.close();
            return "OK";
        } catch (SQLException e) {
            return "ERRO: " + e.toString() + " SQL:" + sql;
        }
    }

    public String geraOutrasDespesas(String codGuia, String cod_convenio, String cod_fatura) {
        String sql = "";
        sql = "INSERT INTO guiasoutrasdespesas(codGuia, numeroFatura, cod_empresa, codigo, tipoTabela, ";
        sql += "descricao, tipoDespesa, dataRealizacao, horaInicial, horaFinal, quantidade, ";
        sql += "valorUnitario, valorTotal) ";
        sql += "SELECT " + codGuia + ", faturas.Numero, outrasdespesas.cod_empresa, outrasdespesas_itens.codigo, outrasdespesas_itens.cod_tabela, ";
        sql += "outrasdespesas_itens.descricao, outrasdespesas_itens.cd, faturas.Data_Lanca, ";
        sql += "faturas.hora_lanca AS hora_inicial, faturas.hora_lanca AS hora_final, faturas_itens.qtde, ";
        sql += "outrasdespesas_itens.valorunitario, (faturas_itens.qtde * outrasdespesas_itens.valorunitario) ";
        sql += "FROM (outrasdespesas_itens INNER JOIN outrasdespesas ON ";
        sql += "outrasdespesas_itens.cod_outrasdespesas = outrasdespesas.cod_outrasdespesas) ";
        sql += "INNER JOIN (faturas_itens INNER JOIN faturas ON faturas_itens.Numero = faturas.Numero) ";
        sql += "ON (outrasdespesas.cod_proced = faturas_itens.Cod_Proced) AND ";
        sql += "(outrasdespesas.cod_convenio = faturas_itens.cod_convenio) ";
        sql += "WHERE outrasdespesas.cod_convenio= " + cod_convenio;
        sql += " AND faturas_itens.Numero=" + cod_fatura;
        return new Banco().executaSQL(sql);
    }

    public String gerarGuiaHonorarioIndividual(String cod_honorario, String usuario) {
        String resp = "";
        String sql = "";
        Banco bc = new Banco();
        ResultSet rs = null;
        if (Util.isNull(cod_honorario)) return "Sem c�digo de honor�rio";
        try {
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String proxcodguia = "", proxnumeroguia = "", cod_empresa = "";
            String data = "", horaInicio = "", horaFim = "";
            sql = "SELECT numeroGuiaPrestador FROM guiashonorarioindividual WHERE cod_honorario=" + cod_honorario;
            rs = stmt.executeQuery(sql);
            if (rs.next()) proxnumeroguia = rs.getString("numeroGuiaPrestador"); else {
                sql = "SELECT planos.cod_convenio FROM honorarios INNER JOIN planos ";
                sql += "ON honorarios.cod_convenio = planos.cod_plano ";
                sql += "WHERE honorarios.cod_honorario=" + cod_honorario;
                String cod_convenio = new Banco().getValor("cod_convenio", sql);
                proxnumeroguia = getProxGuia(cod_convenio, "3");
            }
            sql = "DELETE FROM procedimentoshonorarios WHERE codGuia IN(";
            sql += "SELECT codGuia FROM guiashonorarioindividual WHERE cod_honorario=" + cod_honorario + ")";
            resp = bc.executaSQL(sql);
            if (!resp.equals("OK")) return resp;
            sql = "DELETE FROM guiashonorarioindividual WHERE cod_honorario=" + cod_honorario;
            resp = bc.executaSQL(sql);
            if (!resp.equals("OK")) return resp;
            sql = "SELECT convenio.cod_ans, convenio.cod_convenio, convenio.cod_empresa, ";
            sql += "CASE WHEN tipoidentificadoroperadora='1' THEN 'CNPJ' ELSE CASE WHEN tipoidentificadoroperadora='3' ";
            sql += " THEN 'cpf' ELSE 'codigoPrestadorNaOperadora' END END AS codigoPrestadornaOPeradora, ";
            sql += "honorarios.guiaSolicitacao, honorarios.data, convenio.identificadoroperadora,";
            sql += "paciente_convenio.num_associado_convenio, planos.plano, ";
            sql += "paciente_convenio.validade_carteira, paciente.nome, paciente.cartao_sus, ";
            sql += "hospitais.cnpj, hospitais.*, hospitais.cnes, honorarios.tipoAcomodacao, ";
            sql += "honorarios.data, honorarios.horainicial, honorarios.horafinal, ";
            sql += "honorarios.grauParticipacao, honorarios.obs, profissional.nome, profissao.codCBOS, ";
            sql += "profissional.ufConselho, profissional.reg_prof, profissional.prof_reg, profissao.tipo_registro ";
            sql += "FROM ((((((((honorarios INNER JOIN paciente ON honorarios.codcli = paciente.codcli) ";
            sql += "INNER JOIN paciente_convenio ON (honorarios.cod_convenio = paciente_convenio.cod_plano) ";
            sql += "AND (paciente.codcli = paciente_convenio.codcli)) INNER JOIN planos ON ";
            sql += "paciente_convenio.cod_plano = planos.cod_plano) INNER JOIN convenio ON ";
            sql += "paciente_convenio.cod_convenio = convenio.cod_convenio) INNER JOIN hospitais ON ";
            sql += "honorarios.cod_hospital = hospitais.cod_hospital) INNER JOIN profissional ON ";
            sql += "honorarios.prof_reg = profissional.prof_reg) INNER JOIN prof_esp ON ";
            sql += "profissional.prof_reg = prof_esp.prof_reg) INNER JOIN especialidade ON ";
            sql += "prof_esp.codesp = especialidade.codesp) INNER JOIN profissao ON ";
            sql += "especialidade.cod_profis = profissao.cod_profis ";
            sql += " WHERE honorarios.cod_honorario=" + cod_honorario;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                proxcodguia = bc.getNext("guiashonorarioindividual", "codGuia");
                data = rs.getString("honorarios.data");
                horaInicio = rs.getString("honorarios.horainicial");
                horaFim = rs.getString("honorarios.horafinal");
                Configuracao configuracao = new Configuracao();
                String codigoPrestadornaOPeradora, tipoIdentificacaoContratado, contratado;
                cod_empresa = rs.getString("cod_empresa");
                String pf = getCodProfissional(rs.getString("prof_reg"), rs.getString("cod_convenio"));
                if (Util.isNull(pf)) {
                    tipoIdentificacaoContratado = rs.getString("codigoPrestadornaOPeradora");
                    codigoPrestadornaOPeradora = rs.getString("convenio.identificadoroperadora");
                    contratado = configuracao.getItemConfig("nomeContratado", cod_empresa);
                } else {
                    tipoIdentificacaoContratado = "codigoPrestadorNaOperadora";
                    codigoPrestadornaOPeradora = pf;
                    contratado = rs.getString("profissional.nome");
                }
                sql = "INSERT INTO guiashonorarioindividual ( ";
                sql += "codGuia, cod_empresa, cod_honorario, registroANS, ";
                sql += "dataEmissaoGuia, numeroGuiaPrestador, ";
                sql += "numeroCarteira, nomeBeneficiario, nomePlano, ";
                sql += "validadeCarteira, numeroCNS, identificacaoHospital, tipoIdentificacaoContratado, ";
                sql += "identificacaoContratado, contratado, nomeContratado, ";
                sql += "tipoLogradouro, logradouro, numero, ";
                sql += "codigoIBGEMunicipio, codigoUF, cep, ";
                sql += "numeroCNES, nomeProfissional, siglaConselho, ";
                sql += "numeroConselho, ufConselho, cbos, ";
                sql += "observacao, posicaoProfissional, tipoAcomodacao, numeroGuiaPrincipal, usuario ) VALUES (";
                sql += proxcodguia + "," + cod_empresa + "," + cod_honorario + "," + Util.trataValorNulo(rs.getString("cod_ans")) + ",'";
                sql += rs.getString("data") + "','" + proxnumeroguia + "','";
                sql += rs.getString("num_associado_convenio") + "','" + rs.getString("paciente.nome") + "','" + rs.getString("plano") + "','";
                sql += rs.getString("validade_carteira") + "'," + Util.trataValorNulo(rs.getString("cartao_sus")) + "," + Util.trataValorNulo(Util.soNumeros(rs.getString("cnpj"))) + ",'" + tipoIdentificacaoContratado + "','";
                sql += codigoPrestadornaOPeradora + "','" + contratado + "'," + Util.trataValorNulo(rs.getString("hospitais.descricao")) + ",";
                sql += Util.trataValorNulo(rs.getString("tipoLogradouro")) + "," + Util.trataValorNulo(rs.getString("logradouro")) + "," + Util.trataValorNulo(rs.getString("numero")) + ",";
                sql += Util.trataValorNulo(rs.getString("codigoIBGEMunicipio")) + "," + Util.trataValorNulo(rs.getString("uf")) + "," + Util.trataValorNulo(rs.getString("cep")) + ",";
                sql += Util.trataValorNulo(rs.getString("cnes")) + ",'" + rs.getString("profissional.nome") + "','" + rs.getString("tipo_registro") + "','";
                sql += rs.getString("reg_prof") + "','" + rs.getString("profissional.ufConselho") + "','" + rs.getString("codCBOS") + "','";
                sql += rs.getString("honorarios.obs") + "','" + rs.getString("honorarios.grauParticipacao") + "','";
                sql += Util.trataNulo(rs.getString("tipoAcomodacao"), "") + "','" + rs.getString("guiaSolicitacao") + "','" + usuario + "')";
                resp = bc.executaSQL(sql);
            }
            if (!resp.equals("OK")) return resp;
            sql = "INSERT INTO procedimentoshonorarios ";
            sql += "( codguia, cod_empresa, ";
            sql += "codigo, tipoTabela, descricao, ";
            sql += "data, horaInicio, horaFim, ";
            sql += "quantidadeRealizada, viaAcesso, valor, ";
            sql += "valorTotal ) ";
            sql += "SELECT " + proxcodguia + "," + cod_empresa + ",";
            sql += "procedimentos.CODIGO, valorprocedimentos.cod_tabela, SUBSTR(procedimentos.Procedimento,1,60) AS Procedimento, ";
            sql += "honorario_item.data, '" + horaInicio + "','" + horaFim + "',";
            sql += "honorario_item.qtde, honorario_item.viaAcesso, honorario_item.valor,";
            sql += "(honorario_item.qtde*honorario_item.valor) FROM valorprocedimentos INNER JOIN ((honorario_item ";
            sql += "INNER JOIN procedimentos ON honorario_item.cod_proced = procedimentos.COD_PROCED) ";
            sql += "INNER JOIN honorarios ON honorario_item.cod_honorario = honorarios.cod_honorario) ";
            sql += "ON (valorprocedimentos.cod_plano = honorarios.cod_convenio) AND ";
            sql += "(valorprocedimentos.cod_proced = honorario_item.cod_proced) ";
            sql += "WHERE honorario_item.cod_honorario = " + cod_honorario;
            resp = bc.executaSQL(sql);
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String gerarGuiaResumoInternacao(String cod_resumointernacao, String usuario, String cod_empresa) {
        String resp = "";
        String sql = "";
        Banco bc = new Banco();
        ResultSet rs = null;
        if (Util.isNull(cod_resumointernacao)) return "Sem c�digo de guia";
        try {
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String proxnumeroguia = "", codCNES = "", cod_convenio = "";
            String codigoPrestadornaOPeradora, tipoIdentificacaoContratado, contratado;
            sql = "SELECT numeroGuiaPrestador, usuario FROM guiasresumointernacao WHERE codGuia=" + cod_resumointernacao;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                proxnumeroguia = rs.getString("numeroGuiaPrestador");
                usuario = rs.getString("usuario");
            } else {
                sql = "SELECT planos.cod_convenio FROM resumointernacao INNER JOIN planos ";
                sql += "ON resumointernacao.cod_convenio = planos.cod_plano ";
                sql += "WHERE resumointernacao.cod_resumointernacao=" + cod_resumointernacao;
                cod_convenio = new Banco().getValor("cod_convenio", sql);
                proxnumeroguia = getProxGuia(cod_convenio, "4");
            }
            sql = "DELETE FROM procedimentoresumointernacaoguia WHERE codGuia=" + cod_resumointernacao + " AND cod_empresa=" + cod_empresa;
            resp = bc.executaSQL(sql);
            if (!resp.equals("OK")) return resp;
            sql = "DELETE FROM prof_resumointernacaoguia WHERE cod_resumointernacao=" + cod_resumointernacao + " AND cod_empresa=" + cod_empresa;
            resp = bc.executaSQL(sql);
            if (!resp.equals("OK")) return resp;
            sql = "DELETE FROM guiasresumointernacao WHERE codGuia=" + cod_resumointernacao + " AND cod_empresa=" + cod_empresa;
            resp = bc.executaSQL(sql);
            if (!resp.equals("OK")) return resp;
            sql = "SELECT convenio.cod_ans, resumointernacao.*, paciente_convenio.num_associado_convenio, ";
            sql += "paciente.nome, planos.plano, paciente_convenio.validade_carteira, paciente.cartao_sus, ";
            sql += "convenio.identificadoroperadora, convenio.cod_convenio AS codigoconvenio, ";
            sql += "CASE WHEN tipoidentificadoroperadora='1' THEN 'CNPJ' ELSE CASE WHEN tipoidentificadoroperadora='3' ";
            sql += " THEN 'cpf' ELSE 'codigoPrestadorNaOperadora' END END AS codigoPrestadornaOPeradora ";
            sql += "FROM (((paciente INNER JOIN paciente_convenio ON paciente.codcli = paciente_convenio.codcli) ";
            sql += "INNER JOIN resumointernacao ON paciente.codcli = resumointernacao.codcli) ";
            sql += "INNER JOIN planos ON (planos.cod_plano = paciente_convenio.cod_plano) AND ";
            sql += "(resumointernacao.cod_convenio = planos.cod_plano)) INNER JOIN convenio ON ";
            sql += "planos.cod_convenio = convenio.cod_convenio ";
            sql += "WHERE resumointernacao.cod_resumointernacao = " + cod_resumointernacao;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                Configuracao configuracao = new Configuracao();
                cod_convenio = rs.getString("codigoconvenio");
                tipoIdentificacaoContratado = rs.getString("codigoPrestadornaOPeradora");
                codigoPrestadornaOPeradora = rs.getString("convenio.identificadoroperadora");
                contratado = configuracao.getItemConfig("nomeContratado", cod_empresa);
                codCNES = configuracao.getItemConfig("codCNES", cod_empresa);
                sql = "INSERT INTO guiasresumointernacao ( codGuia, cod_empresa, registroANS, ";
                sql += "dataEmissaoGuia, numeroGuiaPrestador, numeroGuiaSolicitacao, numeroCarteira, ";
                sql += "nomeBeneficiario, nomePlano, validadeCarteira, numeroCNS, tipoIdentificacaoContratado, ";
                sql += "identificacaoContratado, contratado, codCNES, dataAutorizacao, senhaAutorizacao, ";
                sql += "validadeSenha, caraterInternacao, acomodacao, dataHoraInternacao, ";
                sql += "dataHoraSaidaInternacao, tipoInternacao, regimeInternacao, emGestacao, ";
                sql += "aborto, transtornoMaternoRelGravidez, complicacaoPeriodoPuerperio, ";
                sql += "atendimentoRNSalaParto, complicacaoNeonatal, baixoPeso, partoCesareo, ";
                sql += "partoNormal, obitoMulher, declaracoesNascidosVivos, qtdNascidosVivosTermo, ";
                sql += "qtdNascidosMortos, qtdVivosPrematuros, qtdeobitoPrecoce, qtdeobitoTardio, ";
                sql += "codigoDiagnostico, tipoFaturamento, indicadorAcidente, motivoSaidaInternacao, usuario) VALUES (";
                sql += cod_resumointernacao + "," + cod_empresa + "," + Util.trataValorNulo(rs.getString("cod_ans")) + ",'";
                sql += rs.getString("data") + "','" + proxnumeroguia + "'," + Util.trataValorNulo(rs.getString("guiaSolicitacao"));
                sql += ",'" + rs.getString("num_associado_convenio") + "','";
                sql += rs.getString("paciente.nome") + "','" + rs.getString("plano") + "','" + rs.getString("validade_carteira");
                sql += "'," + Util.trataValorNulo(rs.getString("cartao_sus")) + ",'" + tipoIdentificacaoContratado + "','";
                sql += codigoPrestadornaOPeradora + "','" + contratado + "','" + codCNES + "',";
                sql += Util.trataValorNulo(rs.getString("dataAutorizacao")) + "," + Util.trataValorNulo(rs.getString("senhaAutorizacao")) + ",";
                sql += Util.trataValorNulo(rs.getString("validadeSenha")) + ",'" + rs.getString("caraterInternacao") + "','" + rs.getString("tipoAcomodacao");
                sql += "','" + rs.getString("dataInternacao") + "T" + Util.formataHoraHHMMSS(rs.getString("horaInternacao")) + "','";
                sql += rs.getString("dataSaidaInternacao") + "T" + Util.formataHoraHHMMSS(rs.getString("horaSaidaInternacao")) + "',";
                sql += Util.trataValorNulo(rs.getString("tipoInternacao")) + "," + Util.trataValorNulo(rs.getString("regimeInternacao")) + ",";
                sql += Util.trataValorNulo(rs.getString("emGestacao")) + "," + Util.trataValorNulo(rs.getString("aborto")) + ",";
                sql += Util.trataValorNulo(rs.getString("transtornoMaternoRelGravidez")) + "," + Util.trataValorNulo(rs.getString("complicacaoPeriodoPuerperio")) + ",";
                sql += Util.trataValorNulo(rs.getString("atendimentoRNSalaParto")) + "," + Util.trataValorNulo(rs.getString("complicacaoNeonatal")) + ",";
                sql += Util.trataValorNulo(rs.getString("baixoPeso")) + "," + Util.trataValorNulo(rs.getString("partoCesareo")) + ",";
                sql += Util.trataValorNulo(rs.getString("partoNormal")) + "," + Util.trataValorNulo(rs.getString("obitoMulher")) + ",";
                sql += Util.trataValorNulo(rs.getString("declaracoesNascidosVivos")) + "," + Util.trataValorNulo(rs.getString("qtdNascidosVivosTermo")) + ",";
                sql += Util.trataValorNulo(rs.getString("qtdNascidosMortos")) + "," + Util.trataValorNulo(rs.getString("qtdVivosPrematuros")) + ",";
                sql += Util.trataValorNulo(rs.getString("qtdeobitoPrecoce")) + "," + Util.trataValorNulo(rs.getString("qtdeobitoTardio")) + ",";
                sql += Util.trataValorNulo(rs.getString("diagnosticoPrincipal")) + "," + Util.trataValorNulo(rs.getString("tipoFaturamento"));
                sql += "," + Util.trataValorNulo(rs.getString("indicadorAcidente")) + "," + Util.trataValorNulo(rs.getString("motivoSaidaInternacao")) + ",'" + usuario + "')";
                resp = bc.executaSQL(sql);
            } else {
                return "N�o encontrei o registro SQL:" + sql;
            }
            if (!resp.equals("OK")) return resp;
            sql = "INSERT INTO procedimentoresumointernacaoguia ";
            sql += "( codGuia, cod_empresa, ";
            sql += "codigo, tipoTabela, descricao, ";
            sql += "data, horaInicio, horaFim, ";
            sql += "quantidadeRealizada, viaAcesso, valor, ";
            sql += "valorTotal ) ";
            sql += "SELECT " + cod_resumointernacao + "," + cod_empresa + ",";
            sql += "procedimentos.CODIGO, valorprocedimentos.cod_tabela, SUBSTR(procedimentos.Procedimento,1,60) AS Procedimento, ";
            sql += "procedimentosresumointernacao.data, procedimentosresumointernacao.horaInicio,procedimentosresumointernacao.horaFim,";
            sql += "procedimentosresumointernacao.quantidadeRealizada, procedimentosresumointernacao.viaAcesso, procedimentosresumointernacao.valor,";
            sql += "(procedimentosresumointernacao.quantidadeRealizada*procedimentosresumointernacao.valor) FROM valorprocedimentos INNER JOIN ((procedimentosresumointernacao ";
            sql += "INNER JOIN procedimentos ON procedimentosresumointernacao.cod_proced = procedimentos.COD_PROCED) ";
            sql += "INNER JOIN resumointernacao ON procedimentosresumointernacao.codGuia = resumointernacao.cod_resumointernacao) ";
            sql += "ON (valorprocedimentos.cod_plano = resumointernacao.cod_convenio) AND ";
            sql += "(valorprocedimentos.cod_proced = procedimentosresumointernacao.cod_proced) ";
            sql += "WHERE procedimentosresumointernacao.codGuia = " + cod_resumointernacao;
            resp = bc.executaSQL(sql);
            if (!resp.equals("OK")) return resp;
            String total = bc.getValor("total", "SELECT SUM(valorTotal) AS total FROM procedimentoresumointernacaoguia WHERE codGuia=" + cod_resumointernacao + " AND cod_empresa=" + cod_empresa);
            resp = bc.executaSQL("UPDATE guiasresumointernacao SET totalGeral=" + total + " WHERE codGuia=" + cod_resumointernacao + " AND cod_empresa=" + cod_empresa);
            if (!resp.equals("OK")) return resp;
            sql = "SELECT profissional.nome, profissao.tipo_registro, profissional.reg_prof, profissional.ufConselho, ";
            sql += "profissao.codCBOS, prof_resumointernacao.grauParticipacao, prof_resumointernacao.prof_reg ";
            sql += "FROM (prof_resumointernacao INNER JOIN profissional ON prof_resumointernacao.prof_reg = ";
            sql += "profissional.prof_reg) INNER JOIN (profissao RIGHT JOIN (prof_esp LEFT JOIN especialidade ON ";
            sql += "prof_esp.codesp = especialidade.codesp) ON profissao.cod_profis = especialidade.cod_profis) ON ";
            sql += "prof_resumointernacao.prof_reg = prof_esp.prof_reg ";
            sql += "WHERE prof_resumointernacao.cod_resumointernacao=" + cod_resumointernacao;
            sql += " ORDER BY cod_profresumointernacao";
            rs = stmt.executeQuery(sql);
            String codigoProfissional = "";
            while (rs.next()) {
                String pf = getCodProfissional(rs.getString("prof_reg"), cod_convenio);
                if (!Util.isNull(pf)) {
                    codigoProfissional = pf;
                } else {
                    codigoProfissional = codigoPrestadornaOPeradora;
                }
                sql = "INSERT INTO prof_resumointernacaoguia(cod_resumointernacao, cod_empresa, codigoPrestadorNaOperadora,nomeExecutante, siglaConselho, ";
                sql += "numeroConselho, ufConselho, codigoCBOS, posicaoProfissional) VALUES(";
                sql += cod_resumointernacao + "," + cod_empresa + ",'" + codigoProfissional + "','" + rs.getString("nome") + "',";
                sql += Util.trataValorNulo(rs.getString("tipo_registro")) + "," + Util.trataValorNulo(rs.getString("reg_prof")) + ",";
                sql += Util.trataValorNulo(rs.getString("ufConselho")) + "," + Util.trataValorNulo(rs.getString("codCBOS")) + ",";
                sql += Util.trataValorNulo(rs.getString("grauParticipacao")) + ")";
                resp = bc.executaSQL(sql);
            }
            rs.close();
            return "OK";
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String apagarGuia(String cod_fatura, String cod_empresa) {
        String sql = "";
        Banco bc = new Banco();
        sql = "DELETE procedimentossadt.* FROM procedimentossadt INNER JOIN guiassadt ON ";
        sql += "procedimentossadt.codguia = guiassadt.codGuia ";
        sql += "WHERE guiassadt.numeroFatura=" + cod_fatura;
        bc.executaSQL(sql);
        bc.executaSQL("DELETE FROM guiassadt WHERE numeroFatura=" + cod_fatura + " AND cod_empresa=" + cod_empresa);
        bc.executaSQL("DELETE FROM guiasconsulta WHERE numeroFatura=" + cod_fatura + " AND cod_empresa=" + cod_empresa);
        bc.executaSQL("DELETE FROM guiasoutrasdespesas WHERE numeroFatura=" + cod_fatura + " AND cod_empresa=" + cod_empresa);
        return "OK";
    }

    public String atualizaTotais(String codGuia, String cod_empresa) {
        String sql = "";
        ResultSet rs = null;
        float somas[] = new float[7];
        float totalgeral = 0;
        try {
            stmt = con.createStatement();
            sql = "SELECT SUM(faturas_itens.valor*faturas_itens.qtde) AS soma ";
            sql += "FROM (guiassadt INNER JOIN procedimentossadt ON guiassadt.codGuia = ";
            sql += "procedimentossadt.codguia) INNER JOIN faturas_itens ON ";
            sql += "procedimentossadt.codsubitem = faturas_itens.cod_subitem ";
            sql += "WHERE guiassadt.codGuia=" + codGuia;
            sql += " AND guiassadt.cod_empresa=" + cod_empresa;
            rs = stmt.executeQuery(sql);
            if (rs.next()) somas[0] = rs.getFloat("soma");
            totalgeral += somas[0];
            for (int i = 0; i < vetorCD.length; i++) {
                sql = "SELECT SUM(valorUnitario*quantidade) AS soma FROM guiasoutrasdespesas ";
                sql += "WHERE codGuia=" + codGuia + " AND cod_empresa=" + cod_empresa;
                sql += " AND tipoDespesa=" + (i + 1);
                rs = stmt.executeQuery(sql);
                if (rs.next()) somas[i + 1] = rs.getFloat("soma");
                totalgeral += somas[i + 1];
            }
            sql = "UPDATE guiassadt SET servicosexecutados=" + somas[0];
            sql += ", taxas=" + (somas[4] + somas[6]);
            sql += ", materiais=" + somas[3];
            sql += ", medicamentos=" + somas[2];
            sql += ", gases=" + somas[1];
            sql += ", diarias=" + somas[5];
            sql += ", totalGeral=" + totalgeral + " WHERE codGuia=" + codGuia;
            sql += " AND cod_empresa=" + cod_empresa;
            rs.close();
            stmt.close();
            return new Banco().executaSQL(sql);
        } catch (SQLException e) {
            return "ERRO: " + e.toString() + " SQL:" + sql;
        }
    }

    public String getLotes(String cod_empresa, String tipoGuia) {
        String resp = "";
        String sql = "";
        ResultSet rs = null;
        sql += "SELECT codLote FROM lotesguias WHERE gerouGuia='N' ";
        sql += "AND tipoGuia=" + tipoGuia + " AND cod_empresa=" + cod_empresa;
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                resp += "<option value='" + rs.getString("codLote");
                resp += "'>" + rs.getString("codLote") + "</option>";
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    private String getProxGuia(String cod_convenio, String tipoGuia) {
        String resp = "";
        String sql = "";
        String tabela = "";
        ResultSet rs = null;
        try {
            if (tipoGuia.equals("1")) tabela = "guiasconsulta"; else if (tipoGuia.equals("2")) tabela = "guiassadt"; else if (tipoGuia.equals("3")) tabela = "guiashonorarioindividual"; else if (tipoGuia.equals("4")) tabela = "guiasresumointernacao";
            sql = "SELECT MAX(numeroGuiaPrestador)+1 AS prox ";
            sql += "FROM " + tabela + " WHERE registroANS IN (";
            sql += "SELECT convenio.cod_ans FROM convenio ";
            sql += "WHERE cod_convenio=" + cod_convenio + ")";
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                resp = rs.getLong("prox") + "";
            }
            if (resp.equals("0")) resp = "1";
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    private void carregaGuiasAnteriores(String cod_fatura) {
        String sql = "";
        anteriores = new Vector();
        try {
            ResultSet rs = null;
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            sql = "SELECT numeroGuiaPrestador, cod_convenio, tipoConsulta, tipoSaida, codLote, observacao, usuario ";
            sql += "FROM guiasconsulta INNER JOIN convenio ON guiasconsulta.registroANS = convenio.cod_ans ";
            sql += "WHERE numeroFatura=" + cod_fatura;
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                GuiasCadastradas novo = new GuiasCadastradas();
                novo.setCod_convenio(rs.getString("cod_convenio"));
                novo.setNumeroGuia(rs.getString("numeroGuiaPrestador"));
                novo.setTipoConsulta(rs.getString("tipoConsulta"));
                novo.setTipoSaida(rs.getString("tipoSaida"));
                novo.setLote(rs.getString("codLote"));
                novo.setTipoGuia("1");
                novo.setObs(rs.getString("observacao"));
                novo.setUsuario(rs.getString("usuario"));
                anteriores.add(novo);
            }
            sql = "SELECT numeroGuiaPrestador, cod_convenio, caraterAtendimento, tipoAtendimento, tipoSaida, ";
            sql += "dataAutorizacao, senhaAutorizacao, validadeSenha, codLote, observacao, usuario, codigoDiagnostico ";
            sql += "FROM guiassadt INNER JOIN convenio ON guiassadt.registroANS = convenio.cod_ans ";
            sql += "WHERE numeroFatura=" + cod_fatura;
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                GuiasCadastradas novo = new GuiasCadastradas();
                novo.setCod_convenio(rs.getString("cod_convenio"));
                novo.setNumeroGuia(rs.getString("numeroGuiaPrestador"));
                novo.setCaraterAtendimento(rs.getString("caraterAtendimento"));
                novo.setTipoAtendimento(rs.getString("tipoAtendimento"));
                novo.setTipoSaida(rs.getString("tipoSaida"));
                novo.setDataAutorizacao(rs.getString("dataAutorizacao"));
                novo.setSenhaAutorizacao(rs.getString("senhaAutorizacao"));
                novo.setValidadeSenha(rs.getString("validadeSenha"));
                novo.setLote(rs.getString("codLote"));
                novo.setTipoGuia("2");
                novo.setObs(rs.getString("observacao"));
                novo.setUsuario(rs.getString("usuario"));
                novo.setCodigoDiagnostico(rs.getString("codigoDiagnostico"));
                anteriores.add(novo);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println(e.toString());
        }
    }

    private GuiasCadastradas getValoresGuia(String cod_convenio, String tipoGuia) {
        GuiasCadastradas resp;
        if (anteriores == null) return null;
        for (int i = 0; i < anteriores.size(); i++) {
            resp = (GuiasCadastradas) anteriores.get(i);
            if (resp.getCod_convenio().equals(cod_convenio) && resp.getTipoGuia().equals(tipoGuia)) return resp;
        }
        return null;
    }

    public String getProcedimentosSolicitados(String codGuia, String cod_empresa) {
        String resp = "";
        String sql = "";
        ResultSet rs = null;
        sql += "SELECT * FROM procedimentossadt WHERE codGuia=" + codGuia;
        sql += " AND cod_empresa=" + cod_empresa;
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                resp += "<tr>\n";
                resp += " <td class='texto'>" + rs.getString("tipoTabela") + "</td>\n";
                resp += " <td class='texto'>" + rs.getString("codigo") + "</td>\n";
                resp += " <td class='texto'>" + rs.getString("descricao") + "</td>\n";
                resp += " <td class='texto'>" + rs.getString("quantidadeRealizada") + "</td>\n";
                resp += " <td class='texto'>" + rs.getString("quantidadeRealizada") + "</td>\n";
                resp += "</tr>\n";
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String getProcedimentosExecutados(String codGuia, String cod_empresa) {
        String resp = "";
        String sql = "";
        ResultSet rs = null;
        sql += "SELECT * FROM procedimentossadt WHERE codGuia=" + codGuia;
        sql += " AND cod_empresa=" + cod_empresa;
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                resp += "<tr>\n";
                resp += " <td class='texto'>" + Util.formataData(rs.getString("data")) + "</td>\n";
                resp += " <td class='texto'>" + Util.formataHora(rs.getString("horaInicio")) + "</td>\n";
                resp += " <td class='texto'>" + Util.formataHora(rs.getString("horaFim")) + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("tipoTabela"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("codigo"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("descricao"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("quantidadeRealizada"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='center'>" + Util.trataNulo(rs.getString("viaAcesso"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("tecnicaUtilizada"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("reducaoAcrescimo"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("valor"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("valorTotal"), "&nbsp;") + "</td>\n";
                resp += "</tr>\n";
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String getProcedimentosExecutadosHonorarioIndividual(String codGuia, String cod_empresa) {
        String resp = "";
        String sql = "";
        ResultSet rs = null;
        sql += "SELECT * FROM procedimentoshonorarios WHERE codGuia=" + codGuia;
        sql += " AND cod_empresa=" + cod_empresa;
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            int cont = 1;
            float soma = 0;
            while (rs.next()) {
                resp += "<tr>\n";
                resp += " <td class='texto' align='center'>" + cont + "</td>\n";
                resp += " <td class='texto'>" + Util.formataData(rs.getString("data")) + "</td>\n";
                resp += " <td class='texto'>" + Util.formataHora(rs.getString("horaInicio")) + "</td>\n";
                resp += " <td class='texto'>" + Util.formataHora(rs.getString("horaFim")) + "</td>\n";
                resp += " <td class='texto' align='center'>" + Util.trataNulo(rs.getString("tipoTabela"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("codigo"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("descricao"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='center'>" + Util.trataNulo(rs.getString("quantidadeRealizada"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='center'>" + Util.trataNulo(rs.getString("viaAcesso"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("tecnicaUtilizada"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("reducaoAcrescimo"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='right'>" + Util.trataNulo(rs.getString("valor"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='right'>" + Util.trataNulo(rs.getString("valorTotal"), "&nbsp;") + "</td>\n";
                resp += "</tr>\n";
                resp += "<tr><td colspan='13' height='8px'></td></tr>\n";
                cont++;
                soma += rs.getFloat("valorTotal");
            }
            for (int i = cont; i <= 10; i++) {
                resp += "<tr>\n";
                resp += " <td class='texto' align='center'>" + i + "</td>\n";
                resp += " <td class='texto'>&nbsp;</td>\n";
                resp += " <td class='texto'>&nbsp;</td>\n";
                resp += " <td class='texto'>&nbsp;</td>\n";
                resp += " <td class='texto' align='center'>&nbsp;</td>\n";
                resp += " <td class='texto'>&nbsp;</td>\n";
                resp += " <td class='texto'>&nbsp;</td>\n";
                resp += " <td class='texto' align='center'>&nbsp;</td>\n";
                resp += " <td class='texto' align='center'>&nbsp;</td>\n";
                resp += " <td class='texto'>&nbsp;</td>\n";
                resp += " <td class='texto'>&nbsp;</td>\n";
                resp += " <td class='texto'>&nbsp;</td>\n";
                resp += " <td class='texto'>&nbsp;</td>\n";
                resp += "</tr>\n";
                resp += "<tr><td colspan='13' height='8px'></td></tr>\n";
            }
            resp += "<tr>\n";
            resp += "  <td colspan='12'>&nbsp;</td>\n";
            resp += "  <td>\n";
            resp += "    <table cellspacing='0' cellpadding='0' class='tabelaescura' width='100%'>\n";
            resp += "       <tr>\n";
            resp += "         <td class='texto' width='100%'>35-Total Geral Honor�rio R$</td>\n";
            resp += "       </tr>\n";
            resp += "       <tr>\n";
            resp += "         <td class='texto' align='right'><b>" + Util.formatCurrency(soma + "") + "</b></td>\n";
            resp += "       </tr>\n";
            resp += "    </table>\n";
            resp += "   </td>\n";
            resp += "</tr>\n";
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String getProcedimentosExecutadosResumoInternacao(String codGuia, String cod_empresa) {
        String resp = "";
        String sql = "";
        ResultSet rs = null;
        sql += "SELECT * FROM procedimentoresumointernacaoguia WHERE codGuia=" + codGuia;
        sql += " AND cod_empresa=" + cod_empresa;
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            int cont = 1;
            float soma = 0;
            while (rs.next()) {
                resp += "<tr>\n";
                resp += " <td class='texto' align='center'>" + cont + "</td>\n";
                resp += " <td class='texto'>" + Util.formataData(rs.getString("data")) + "</td>\n";
                resp += " <td class='texto'>" + Util.formataHora(rs.getString("horaInicio")) + "</td>\n";
                resp += " <td class='texto'>" + Util.formataHora(rs.getString("horaFim")) + "</td>\n";
                resp += " <td class='texto' align='center'>" + Util.trataNulo(rs.getString("tipoTabela"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("codigo"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("descricao"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='center'>" + Util.trataNulo(rs.getString("quantidadeRealizada"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='center'>" + Util.trataNulo(rs.getString("viaAcesso"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("tecnicaUtilizada"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("reducaoAcrescimo"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='right'>" + Util.trataNulo(rs.getString("valor"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='right'>" + Util.trataNulo(rs.getString("valorTotal"), "&nbsp;") + "</td>\n";
                resp += "</tr>\n";
                resp += "<tr><td colspan='13' height='8px'></td></tr>\n";
                cont++;
                soma += rs.getFloat("valorTotal");
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String getProfEquipeResumoInternacao(String codGuia, String cod_empresa) {
        String resp = "";
        String sql = "";
        ResultSet rs = null;
        sql += "SELECT * FROM prof_resumointernacaoguia WHERE cod_resumointernacao=" + codGuia + " AND cod_empresa=" + cod_empresa;
        try {
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            int cont = 1;
            while (rs.next()) {
                resp += "<tr>\n";
                resp += " <td class='texto' align='center'>" + cont + "</td>\n";
                resp += " <td class='texto' align='center'>" + rs.getString("posicaoProfissional") + "</td>\n";
                resp += " <td class='texto'>" + rs.getString("codigoPrestadorNaOperadora") + "</td>\n";
                resp += " <td class='texto'>" + rs.getString("nomeExecutante") + "</td>\n";
                resp += " <td class='texto' align='center'>" + Util.trataNulo(rs.getString("siglaConselho"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto'>" + Util.trataNulo(rs.getString("numeroConselho"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='center'>" + Util.trataNulo(rs.getString("ufConselho"), "&nbsp;") + "</td>\n";
                resp += " <td class='texto' align='center'>&nbsp</td>\n";
                resp += "</tr>\n";
                cont++;
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String getCodProfOperadora(String prof_reg, String cod_convenio) {
        String resp = "";
        String sql = "";
        ResultSet rs = null;
        try {
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            sql = "SELECT * FROM prof_convenio WHERE prof_reg='" + prof_reg + "' AND cod_convenio=" + cod_convenio;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                resp = rs.getString("codOperadora");
            }
            rs.close();
            stmt.close();
            return resp;
        } catch (SQLException e) {
            resp = "ERRO: " + e.toString() + " SQL:" + sql;
            return resp;
        }
    }

    public String getHashCalculado(String w_ac) {
        try {
            String wmd = Util.digest(w_ac, "ISO8859_1");
            return wmd;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void main(String args[]) {
        TISS tiss = new TISS();
        String teste = "";
        teste = tiss.gerarGuia("20304", "71", "");
        System.out.println("Result==>" + teste);
    }
}

class GuiasCadastradas {

    private String cod_convenio;

    private String tipoGuia;

    private String numeroGuia;

    private String tipoAtendimento;

    private String tipoConsulta;

    private String tipoSaida;

    private String caraterAtendimento;

    private String dataAutorizacao;

    private String senhaAutorizacao;

    private String validadeSenha;

    private String lote;

    private String obs;

    private String usuario;

    private String codigoDiagnostico;

    public String getCod_convenio() {
        return cod_convenio;
    }

    public void setCod_convenio(String cod_convenio) {
        this.cod_convenio = cod_convenio;
    }

    public String getTipoGuia() {
        return tipoGuia;
    }

    public void setTipoGuia(String tipoGuia) {
        this.tipoGuia = tipoGuia;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public String getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(String tipoAtendimento) {
        this.tipoAtendimento = tipoAtendimento;
    }

    public String getTipoConsulta() {
        return tipoConsulta;
    }

    public void setTipoConsulta(String tipoConsulta) {
        this.tipoConsulta = tipoConsulta;
    }

    public String getTipoSaida() {
        return tipoSaida;
    }

    public void setTipoSaida(String tipoSaida) {
        this.tipoSaida = tipoSaida;
    }

    public String getCaraterAtendimento() {
        return caraterAtendimento;
    }

    public void setCaraterAtendimento(String caraterAtendimento) {
        this.caraterAtendimento = caraterAtendimento;
    }

    public String getDataAutorizacao() {
        return dataAutorizacao;
    }

    public void setDataAutorizacao(String dataAutorizacao) {
        this.dataAutorizacao = dataAutorizacao;
    }

    public String getSenhaAutorizacao() {
        return senhaAutorizacao;
    }

    public void setSenhaAutorizacao(String senhaAutorizacao) {
        this.senhaAutorizacao = senhaAutorizacao;
    }

    public String getValidadeSenha() {
        return validadeSenha;
    }

    public void setValidadeSenha(String validadeSenha) {
        this.validadeSenha = validadeSenha;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public String getObs() {
        return obs;
    }

    public void setObs(String obs) {
        this.obs = obs;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getCodigoDiagnostico() {
        return codigoDiagnostico;
    }

    public void setCodigoDiagnostico(String codigoDiagnostico) {
        this.codigoDiagnostico = codigoDiagnostico;
    }
}
