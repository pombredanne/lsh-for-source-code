package com.eip.yost.services.impl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.eip.yost.commun.exceptions.AffectationInexistanteException;
import com.eip.yost.commun.exceptions.ClientInexistantException;
import com.eip.yost.commun.exceptions.FactureInexistanteException;
import com.eip.yost.commun.exceptions.SouscriptionInexistanteException;
import com.eip.yost.dao.interfaces.IAffectationDao;
import com.eip.yost.dao.interfaces.IClientDao;
import com.eip.yost.dao.interfaces.IConfigPersoDao;
import com.eip.yost.dao.interfaces.IFactureDao;
import com.eip.yost.dao.interfaces.ISouscriptionDao;
import com.eip.yost.dto.AffectationDTO;
import com.eip.yost.dto.ClientDTO;
import com.eip.yost.dto.FactureDTO;
import com.eip.yost.dto.SouscriptionDTO;
import com.eip.yost.entite.Affectation;
import com.eip.yost.entite.Client;
import com.eip.yost.entite.ConfigPerso;
import com.eip.yost.entite.Facture;
import com.eip.yost.entite.Souscription;
import com.eip.yost.services.interfaces.IClientManager;
import com.eip.yost.utils.BeanToDTO;
import com.eip.yost.utils.ReportWriter;

@Service("clientManager")
public class ClientManagerImpl implements IClientManager {

    /** DAO Client **/
    @Autowired(required = true)
    private IClientDao mClientDao;

    /** DAO Souscription **/
    @Autowired(required = true)
    private ISouscriptionDao mSouscriptionDao;

    /** DAO Affectation */
    @Autowired(required = true)
    private IAffectationDao mAffectationDao;

    /** DAO ConfigPerso */
    @Autowired(required = true)
    private IConfigPersoDao mConfigPersoDao;

    /** DAO Facture */
    @Autowired(required = true)
    private IFactureDao mFactureDao;

    /**
	 * {@inheritDoc}
	 */
    public boolean checkLogin(String pMail, String pMdp) {
        boolean vOk = false;
        if (this.exists(pMail)) {
            vOk = mClientDao.checkLogin(pMail, pMdp);
        }
        return vOk;
    }

    /**
	 * {@inheritDoc}
	 */
    public ClientDTO changePassword(String pMail, String pMdp) {
        Client vClientBean = null;
        ClientDTO vClientDTO = null;
        vClientBean = mClientDao.getClient(pMail);
        if (vClientBean != null) {
            MessageDigest vMd5Instance;
            try {
                vMd5Instance = MessageDigest.getInstance("MD5");
                vMd5Instance.reset();
                vMd5Instance.update(pMdp.getBytes());
                byte[] vDigest = vMd5Instance.digest();
                BigInteger vBigInt = new BigInteger(1, vDigest);
                String vHashPassword = vBigInt.toString(16);
                vClientBean.setMdp(vHashPassword);
                vClientDTO = BeanToDTO.getInstance().createClientDTO(vClientBean);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return vClientDTO;
    }

    /**
	 * {@inheritDoc}
	 */
    public ClientDTO getClient(String pMail) throws ClientInexistantException {
        Client vClientBean = null;
        ClientDTO vClientDTO = null;
        try {
            vClientBean = mClientDao.getClient(pMail);
            vClientDTO = BeanToDTO.getInstance().createClientDTO(vClientBean);
            vClientDTO.setSouscriptionList(this.findAllClientSouscription(vClientBean.getIdclient()));
        } catch (EntityNotFoundException e) {
            throw new ClientInexistantException("Le client avec l'adresse mail: " + pMail + " n'existe pas.");
        }
        return vClientDTO;
    }

    /**
	 * {@inheritDoc}
	 */
    public ClientDTO getClient(Integer pId) throws ClientInexistantException {
        Client vClientBean = null;
        ClientDTO vClientDTO = null;
        try {
            vClientBean = mClientDao.get(pId);
            vClientDTO = BeanToDTO.getInstance().createClientDTO(vClientBean);
        } catch (EntityNotFoundException e) {
            throw new ClientInexistantException("Le client avec l'id: " + pId + " n'existe pas.");
        }
        return vClientDTO;
    }

    /**
	 * {@inheritDoc}
	 */
    public List<ClientDTO> getAll() {
        List<Client> vClientBeanList = null;
        List<ClientDTO> vClientDTOList = new ArrayList<ClientDTO>();
        vClientBeanList = mClientDao.getAll();
        if (vClientBeanList != null && vClientBeanList.size() > 0) {
            for (Client vClient : vClientBeanList) {
                ClientDTO vClientDTO = BeanToDTO.getInstance().createClientDTO(vClient);
                vClientDTOList.add(vClientDTO);
            }
        }
        return vClientDTOList;
    }

    /**
	 * {@inheritDoc}
	 */
    public List<ClientDTO> getClientSouscriptionArrivantAEcheance(Integer pNbMois) {
        List<Client> vClientBeanList = null;
        List<ClientDTO> vClientDTOList = new ArrayList<ClientDTO>();
        vClientBeanList = mClientDao.getClientSouscriptionArrivantAEcheance(pNbMois);
        if (vClientBeanList != null && vClientBeanList.size() > 0) {
            for (Client vClient : vClientBeanList) {
                ClientDTO vClientDTO = BeanToDTO.getInstance().createClientDTO(vClient);
                vClientDTOList.add(vClientDTO);
            }
        }
        return vClientDTOList;
    }

    /**
	 * {@inheritDoc}
	 */
    public void remove(Integer pId) {
        mClientDao.remove(pId);
    }

    /**
	 * {@inheritDoc}
	 */
    public boolean exists(String pMail) {
        boolean vExists = false;
        Client vClientBean = null;
        if ((vClientBean = mClientDao.getClient(pMail)) != null) {
            vExists = mClientDao.exists(vClientBean.getIdclient());
        }
        return vExists;
    }

    /**
	 * {@inheritDoc}
	 */
    public void save(Client pClient) {
        mClientDao.save(pClient);
    }

    /**
	 * {@inheritDoc}
	 */
    public List<SouscriptionDTO> findAllClientSouscription(Integer pIdClient) {
        List<Souscription> vSouscriptionBeanList = null;
        List<SouscriptionDTO> vSouscriptionDTOList = new ArrayList<SouscriptionDTO>();
        vSouscriptionBeanList = mSouscriptionDao.findByIdClient(pIdClient);
        if (vSouscriptionBeanList != null && vSouscriptionBeanList.size() > 0) {
            for (Souscription vSouscription : vSouscriptionBeanList) {
                vSouscriptionDTOList.add(BeanToDTO.getInstance().createSouscriptionDTO(vSouscription));
            }
        }
        return vSouscriptionDTOList;
    }

    /**
	 * {@inheritDoc}
	 */
    public SouscriptionDTO getSouscription(Integer pIdSouscription) throws SouscriptionInexistanteException {
        Souscription vSouscriptionBean = null;
        SouscriptionDTO vSouscriptionDTO = null;
        try {
            vSouscriptionBean = mSouscriptionDao.get(pIdSouscription);
            vSouscriptionDTO = BeanToDTO.getInstance().createSouscriptionDTO(vSouscriptionBean);
        } catch (EntityNotFoundException e) {
            throw new SouscriptionInexistanteException("La souscription d'id: " + pIdSouscription + " n'existe pas.");
        }
        return vSouscriptionDTO;
    }

    /**
	 * {@inheritDoc}
	 */
    public Souscription addSouscription(Souscription pSouscription) {
        return mSouscriptionDao.save(pSouscription);
    }

    /**
	 * {@inheritDoc}
	 */
    public AffectationDTO findAffectationByIdSouscriptionAndIdModule(Integer pIdSouscription, Integer pIdModule) throws AffectationInexistanteException {
        Affectation vAffectationBean = null;
        AffectationDTO vAffectationDTO = null;
        try {
            vAffectationBean = mAffectationDao.findAffectationByIdSouscriptionAndIdModule(pIdSouscription, pIdModule);
            vAffectationDTO = BeanToDTO.getInstance().createAffectationDTO(vAffectationBean);
        } catch (EntityNotFoundException e) {
            throw new AffectationInexistanteException("L'affectation correspondante à l'id souscription " + pIdSouscription + "et à l'id module " + pIdModule + " n'existe pas.");
        }
        return vAffectationDTO;
    }

    /**
	 * {@inheritDoc}
	 */
    public Affectation addAffectation(Affectation pAffectation) {
        return mAffectationDao.save(pAffectation);
    }

    /**
	 * {@inheritDoc}
	 */
    public void addConfigPerso(String pCle, String pValeur, Integer pCache, Integer pIdAffectation) {
        ConfigPerso vConfigPerso = new ConfigPerso(pCle, pValeur, pCache, mAffectationDao.get(pIdAffectation));
        mConfigPersoDao.save(vConfigPerso);
    }

    /**
	 * {@inheritDoc}
	 */
    public List<ClientDTO> findByPartialMail(String pPartial, Integer pLimit) {
        List<ClientDTO> vClientDTOList = new ArrayList<ClientDTO>();
        List<Client> vClientBeanList = null;
        ClientDTO vClientDTO = null;
        vClientBeanList = mClientDao.findByPartialMail(pPartial, pLimit);
        if (vClientBeanList != null && vClientBeanList.size() > 0) {
            for (Client vClient : vClientBeanList) {
                vClientDTO = BeanToDTO.getInstance().createClientDTO(vClient);
                vClientDTOList.add(vClientDTO);
            }
        }
        return vClientDTOList;
    }

    /**
	 * {@inheritDoc}
	 */
    public void toggleClient(Integer pClientId) throws ClientInexistantException {
        Client vClientBean = null;
        List<SouscriptionDTO> vSouscriptionDTOList = null;
        try {
            vClientBean = mClientDao.get(pClientId);
            if (vClientBean.getEtat().equals(1)) {
                vClientBean.setEtat(2);
            } else {
                vClientBean.setEtat(1);
            }
            mClientDao.save(vClientBean);
            vSouscriptionDTOList = findAllClientSouscription(pClientId);
            if (vSouscriptionDTOList != null && vSouscriptionDTOList.size() > 0) {
                for (SouscriptionDTO vSouscriptionDTO : vSouscriptionDTOList) {
                    vSouscriptionDTO.setEtat(vClientBean.getEtat());
                    mSouscriptionDao.save(new Souscription(vSouscriptionDTO));
                }
            }
        } catch (EntityNotFoundException e) {
            throw new ClientInexistantException("Le client d'id: " + pClientId + " n'existe pas.");
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void deleteClient(Integer pClientId) throws ClientInexistantException {
        Client vClientBean = null;
        List<SouscriptionDTO> vSouscriptionDTOList = null;
        try {
            vClientBean = mClientDao.get(pClientId);
            vClientBean.setEtat(0);
            mClientDao.save(vClientBean);
            vSouscriptionDTOList = findAllClientSouscription(pClientId);
            if (vSouscriptionDTOList != null && vSouscriptionDTOList.size() > 0) {
                for (SouscriptionDTO vSouscriptionDTO : vSouscriptionDTOList) {
                    vSouscriptionDTO.setEtat(0);
                    mSouscriptionDao.save(new Souscription(vSouscriptionDTO));
                }
            }
        } catch (EntityNotFoundException e) {
            throw new ClientInexistantException("Le client d'id: " + pClientId + " n'existe pas.");
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void toggleSouscription(Integer pSouscriptionId) throws SouscriptionInexistanteException {
        Souscription vSouscriptionBean = null;
        try {
            vSouscriptionBean = mSouscriptionDao.get(pSouscriptionId);
            if (vSouscriptionBean.getEtat().equals(1)) {
                vSouscriptionBean.setEtat(2);
            } else {
                vSouscriptionBean.setEtat(1);
            }
            mSouscriptionDao.save(vSouscriptionBean);
        } catch (EntityNotFoundException e) {
            throw new SouscriptionInexistanteException("La souscription d'id: " + pSouscriptionId + " n'existe pas.");
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void resilierSouscription(Integer pSouscriptionId) throws SouscriptionInexistanteException {
        Souscription vSouscriptionBean = null;
        try {
            vSouscriptionBean = mSouscriptionDao.get(pSouscriptionId);
            vSouscriptionBean.setEtat(0);
            vSouscriptionBean.setDateResiliation(new Date());
            mSouscriptionDao.save(vSouscriptionBean);
        } catch (EntityNotFoundException e) {
            throw new SouscriptionInexistanteException("La souscription d'id: " + pSouscriptionId + " n'existe pas.");
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public List<FactureDTO> getFacture(Integer pClientId) {
        List<Facture> vFactureList = mFactureDao.findFactureByIdClient(pClientId);
        List<FactureDTO> vFactureDTOList = new ArrayList<FactureDTO>();
        if (vFactureList != null && vFactureList.size() > 0) {
            for (Facture vFacture : vFactureList) {
                vFactureDTOList.add(BeanToDTO.getInstance().createFactureDTO(vFacture));
            }
        }
        return vFactureDTOList;
    }

    /**
	 * {@inheritDoc}
	 */
    public void genererFacture(SouscriptionDTO pSouscription, Locale pCurrentLocale) throws Exception {
        List<Affectation> vAffecationList = mAffectationDao.findAffectationByIdSouscription(pSouscription.getIdSouscription());
        List<AffectationDTO> vAffecationDTOList = new ArrayList<AffectationDTO>();
        Facture vFacture = null;
        if (vAffecationList != null && vAffecationList.size() > 0) {
            for (Affectation vAffectaion : vAffecationList) {
                vAffecationDTOList.add(BeanToDTO.getInstance().createAffectationDTO(vAffectaion));
            }
            pSouscription.setAffectationList(vAffecationDTOList);
            vFacture = mFactureDao.getFactureFromIdSouscription(pSouscription.getIdSouscription());
            if (vFacture == null) {
                vFacture = new Facture();
                vFacture.setPaye(0);
                vFacture.setChemin(new ReportWriter().generate(pSouscription, pCurrentLocale));
                vFacture.setSouscription(new Souscription(pSouscription));
                mFactureDao.save(vFacture);
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void toggleFacture(Integer pFactureId) throws FactureInexistanteException {
        Facture vFactureBean = null;
        try {
            vFactureBean = mFactureDao.get(pFactureId);
            if (vFactureBean.getPaye().equals(1)) {
                vFactureBean.setPaye(0);
            } else {
                vFactureBean.setPaye(1);
            }
            mFactureDao.save(vFactureBean);
        } catch (EntityNotFoundException e) {
            throw new FactureInexistanteException("La facture d'id: " + pFactureId + " n'existe pas.");
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void deleteInvoices(Integer pInvoicesId) {
        mFactureDao.remove(pInvoicesId);
    }
}
