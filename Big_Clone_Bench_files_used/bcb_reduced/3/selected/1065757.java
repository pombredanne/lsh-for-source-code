package net.sourceforge.xsurvey.xscreator.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.annotation.Resource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sourceforge.xsurvey.xscreator.dao.SurveyDao;
import net.sourceforge.xsurvey.xscreator.dao.TokenDao;
import net.sourceforge.xsurvey.xscreator.domain.Token;
import net.sourceforge.xsurvey.xscreator.exception.XSServiceException;
import net.sourceforge.xsurvey.xscreator.service.SurveyService;
import net.sourceforge.xsurvey.xscreator.xjc.Survey;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
public class SurveyServiceImpl implements SurveyService {

    public static Logger logger = Logger.getLogger(SurveyServiceImpl.class);

    @Resource
    Jaxb2Marshaller surveyMarshaller;

    @Resource
    SurveyDao surveyDao;

    @Resource
    TokenDao tokenDao;

    @Override
    public String loadSurveyXML(long id) throws XSServiceException {
        try {
            return surveyDao.load(id);
        } catch (DataAccessException e) {
            logger.warn("DataAccessException thrown while getting survey:" + e.getMessage(), e);
            throw new XSServiceException("Database error while getting survey");
        }
    }

    @Override
    public Survey load(long id) throws XSServiceException {
        final String is = surveyDao.load(id);
        final ByteArrayInputStream bais = new ByteArrayInputStream(is.getBytes());
        final Survey s = (Survey) surveyMarshaller.unmarshal(new StreamSource(bais));
        return s;
    }

    @Override
    public void saveOrUpdate(Survey survey) throws XSServiceException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            surveyMarshaller.marshal(survey, new StreamResult(baos));
            if (survey.isSetSid() && survey.getSid() > 0) {
                surveyDao.update(survey.getSid(), survey.getSkin().getId(), survey.getName(), survey.getStartdate(), survey.getEnddate(), survey.getStatus().name(), baos.toString("UTF-8"));
            } else {
                long id = surveyDao.save(survey.getSkin().getId(), survey.getName(), survey.getStartdate(), survey.getEnddate(), survey.getStatus().name(), baos.toString("UTF-8"));
                survey.setSid(id);
                final List<Token> tokens = generateTokens(survey.getUserCount().intValue());
                tokenDao.saveTokensForSurvey(survey.getSid(), tokens);
            }
            baos.close();
        } catch (XmlMappingException e) {
            logger.warn("XmlMappingException thrown while marshalling survey", e);
            throw new XSServiceException("Error while saving survey");
        } catch (IOException e) {
            logger.warn("IOException thrown while marshalling survey", e);
            throw new XSServiceException("Error while saving survey");
        } catch (DataAccessException e) {
            logger.warn("DataAccessException thrown while marshalling survey:" + e.getMessage(), e);
            throw new XSServiceException("Database error while saving survey");
        }
    }

    @Override
    public List<Survey> getSurveys(long limit, long offset, String orderProperty, boolean ascending) throws XSServiceException {
        try {
            final List<String> surveysxml = surveyDao.getSurveys(limit, offset, orderProperty, ascending);
            final List<Survey> surveys = new ArrayList<Survey>(surveysxml.size());
            for (String s : surveysxml) {
                final ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes());
                surveys.add((Survey) surveyMarshaller.unmarshal(new StreamSource(bais)));
            }
            return surveys;
        } catch (DataAccessException e) {
            logger.warn("DataAccessException thrown while marshalling survey:" + e.getMessage(), e);
            e.printStackTrace();
            throw new XSServiceException("Database error while querying for survey list");
        } catch (Exception e) {
            logger.warn("Uknown exception thrown while marshalling survey:" + e.getMessage(), e);
            e.printStackTrace();
            throw new XSServiceException("Uknown error while querying for survey list");
        }
    }

    @Override
    public long getSurveyCount() throws XSServiceException {
        try {
            return surveyDao.getSurveyCount();
        } catch (DataAccessException e) {
            logger.warn("DataAccessException thrown while getting survey count:" + e.getMessage(), e);
            throw new XSServiceException("Database error while getting survey count");
        }
    }

    @Override
    public void deleteSurvey(Survey survey) throws XSServiceException {
        try {
            surveyDao.delete(survey.getSid());
        } catch (DataAccessException e) {
            logger.warn("DataAccessException thrown while deleting survey:" + e.getMessage(), e);
            throw new XSServiceException("Database error while deleting survey");
        } catch (Exception e) {
            logger.warn("Uknown exception thrown while deleting survey:" + e.getMessage(), e);
            throw new XSServiceException("Uknown error while deleting survey");
        }
    }

    private List<Token> generateTokens(int tokenCount) throws XSServiceException {
        final List<Token> tokens = new ArrayList<Token>(tokenCount);
        final Random r = new Random();
        String t = Long.toString(new Date().getTime()) + Integer.toString(r.nextInt());
        final MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new XSServiceException("Error while creating tokens");
        }
        for (int i = 0; i < tokenCount; ++i) {
            final Token token = new Token();
            token.setValid(true);
            m.update(t.getBytes(), 0, t.length());
            String md5 = new BigInteger(1, m.digest()).toString(16);
            while (md5.length() < 32) {
                md5 = String.valueOf(r.nextInt(9)) + md5;
            }
            t = md5.substring(0, 8) + "-" + md5.substring(8, 16) + "-" + md5.substring(16, 24) + "-" + md5.substring(24, 32);
            logger.debug("Generated token #" + (i + 1) + ": " + t);
            token.setTokenString(t);
            tokens.add(token);
        }
        return tokens;
    }

    @Override
    public void updateWithoutContent(Survey survey) throws XSServiceException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Survey toUpdate = load(survey.getSid());
            toUpdate.setAdministrator(survey.getAdministrator());
            toUpdate.setStatus(survey.getStatus());
            toUpdate.setDescription(survey.getDescription());
            toUpdate.setEnddate(survey.getEnddate());
            toUpdate.setName(toUpdate.getName());
            toUpdate.setSkin(survey.getSkin());
            toUpdate.setStartdate(survey.getStartdate());
            toUpdate.setUserCount(survey.getUserCount());
            surveyMarshaller.marshal(toUpdate, new StreamResult(baos));
            surveyDao.update(survey.getSid(), survey.getSkin().getId(), survey.getName(), survey.getStartdate(), survey.getEnddate(), survey.getStatus().name(), baos.toString("UTF-8"));
            baos.close();
        } catch (XmlMappingException e) {
            logger.warn("XmlMappingException thrown while marshalling survey", e);
            throw new XSServiceException("Error while saving survey");
        } catch (IOException e) {
            logger.warn("IOException thrown while marshalling survey", e);
            throw new XSServiceException("Error while saving survey");
        } catch (DataAccessException e) {
            logger.warn("DataAccessException thrown while marshalling survey:" + e.getMessage(), e);
            throw new XSServiceException("Database error while saving survey");
        }
    }
}
