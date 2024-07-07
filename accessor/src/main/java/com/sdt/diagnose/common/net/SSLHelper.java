package com.sdt.diagnose.common.net;

import android.text.TextUtils;
import android.util.Base64;

import com.sdt.diagnose.common.log.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * SSL证书管理器
 */
public class SSLHelper {
    private static final String TAG = "SSLHelper";
    private static final String CHARSET_NAME = "UTF-8";
    private static final String KEY_NAME = "client.bks";
    private static final String BEGIN_KEY = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String END_KEY = "-----END RSA PRIVATE KEY-----";
    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERT = "-----END CERTIFICATE-----";

    /**
     * 用系统服务器证书
     *
     * @param keyString:私钥
     * @param certString:证书
     */
    public static SSLSocketFactory getSslSocketFactory(
            String caCert, String keyString, String certString, String pwd) throws Exception {
        try {
            KeyStore keyStore = buildKeyStore(keyString, certString, pwd);
            // 密钥管理器
            KeyManagerFactory kmFactory = KeyManagerFactory.getInstance("X509");
            kmFactory.init(keyStore, pwd.toCharArray());
            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            if (TextUtils.isEmpty(caCert)) {
                sslContext.init(
                        kmFactory.getKeyManagers(), new TrustManager[]{getTrustManager()}, null);
            } else {
                sslContext.init(
                        kmFactory.getKeyManagers(),
                        getTrustManagerFactory(caCert).getTrustManagers(),
                        null);
            }
            // 获得SSLSocketFactory
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new Exception(e.getLocalizedMessage());
        }
    }

    /**
     * 从文件流中读取证书相关信息,返回SSLSocketFactory
     */
    public static SSLSocketFactory getSslSocketFactory(
            final InputStream in, final String password, final String type) throws Exception {
        try {
            // 设置密钥
            KeyStore clientKeyStore = KeyStore.getInstance(type);
            clientKeyStore.load(in, password.toCharArray());
            // 密钥管理器
            KeyManagerFactory kmFactory = KeyManagerFactory.getInstance("X509");
            kmFactory.init(clientKeyStore, password.toCharArray());
            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    kmFactory.getKeyManagers(), new TrustManager[]{getTrustManager()}, null);
            // 获得SSLSocketFactory
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new Exception(e.getLocalizedMessage());
        }
    }

    /**
     * 构建客户端KeyStore
     */
    private static KeyStore buildKeyStore(String privateKeyPem, String certificatePem, String pwd)
            throws Exception {
        final X509Certificate[] cert = createCertificates(certificatePem);
        try {
            KeyStore keystore = KeyStore.getInstance("BKS");
            keystore.load(null);
            PrivateKey key = createPrivateKey(privateKeyPem);
            keystore.setKeyEntry(KEY_NAME, key, pwd.toCharArray(), cert);
            return keystore;
        } catch (KeyStoreException
                 | CertificateException
                 | NoSuchAlgorithmException
                 | IOException e) {
            LogUtils.e(TAG, "buildKeyStore error: " + e.getMessage());
            throw new Exception(e.getLocalizedMessage());
        }
    }

    /**
     * 创建私钥
     */
    private static PrivateKey createPrivateKey(String privateKey) {
        if (TextUtils.isEmpty(privateKey)) {
            throw new IllegalArgumentException("privateKey is null ");
        }
        privateKey = privateKey.replaceAll("\n", "");
        if (!privateKey.startsWith(BEGIN_KEY) || !privateKey.endsWith(END_KEY)) {
            throw new IllegalArgumentException("privateKey format error");
        }
        privateKey = privateKey.replaceAll(BEGIN_KEY, "").replaceAll(END_KEY, "");

        byte[] bytes = Base64.decode(privateKey, Base64.DEFAULT);
        return generatePrivateKeyFromDER(bytes);
    }

    /**
     * 创建证书,首先去掉\n，然后匹配是否有-----BEGIN CERTIFICATE-----和-----END CERTIFICATE-----
     *
     * <p>-----BEGIN CERTIFICATE----- Base64数据 -----END CERTIFICATE-----
     */
    public static X509Certificate[] createCertificates(String certString) throws Exception {

        final List<X509Certificate> result = new ArrayList<>();
        if (TextUtils.isEmpty(certString)) {
            throw new Exception("cert is null ");
        }
        certString = certString.replaceAll("[\r\n]", "");
        if (!certString.startsWith(BEGIN_CERT) || !certString.endsWith(END_CERT)) {
            throw new Exception("cert format error");
        }

        String[] certs = certString.split(END_CERT);
        for (String hexString : certs) {
            hexString = hexString.replaceAll(BEGIN_CERT, "").replaceAll(END_CERT, "");
            byte[] bytes = Base64.decode(hexString, Base64.DEFAULT);
            X509Certificate cert = generateCertificateFromDER(bytes);
            result.add(cert);
        }
        return result.toArray(new X509Certificate[0]);
    }

    /**
     * 生成RSAPrivateKey
     */
    private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LogUtils.e(TAG, "generatePrivateKeyFromDER error: " + e.getMessage());
        }
        return null;
    }

    /**
     * 生成X509证书
     */
    private static X509Certificate generateCertificateFromDER(byte[] certBytes) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate)
                    factory.generateCertificate(new ByteArrayInputStream(certBytes));
        } catch (CertificateException e) {
            LogUtils.e(TAG, "generateCertificateFromDER error. " + e.getLocalizedMessage());
        }
        return null;
    }

    private static TrustManagerFactory getTrustManagerFactory(String caCertString)
            throws Exception {
        try {
            int index = 0;
            X509Certificate[] x509Certificates = createCertificates(caCertString);
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            for (X509Certificate certificate : x509Certificates) {
                trustStore.setCertificateEntry("ca-certificate" + (index++), certificate);
            }

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            return trustManagerFactory;
        } catch (Exception e) {
            LogUtils.e(TAG, "getTrustManagerFactory error. " + e.getLocalizedMessage());
            throw new Exception(e.getLocalizedMessage());
        }
    }

    /**
     * 获得信任管理器TrustManager,不做任何校验
     *
     * @return X509TrustManager
     */
    public static X509TrustManager getTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] serverX509Certificates, String s) {
            }

            /**
             * 只支持正序或者逆序存放的证书链，如果证书链顺序打乱的将不支持 我们以下认定x509Certificates数组里从0-end如果是设备证书到ca root证书是正序的
             * 反之是倒序的
             */
            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    /**
     * 实现手动对服务器证书的校验，供特殊需求使用
     *
     * @return X509TrustManager
     */
    public static X509TrustManager getTrustManager(final String caCertString) {

        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] serverX509Certificates, String s) {
            }

            /**
             * 只支持正序或者逆序存放的证书链，如果证书链顺序打乱的将不支持 我们以下认定x509Certificates数组里从0-end如果是设备证书到ca root证书是正序的
             * 反之是倒序的
             */
            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                    throws CertificateException {

                X509Certificate[] x509CertificatesTmp = new X509Certificate[0];
                try {
                    x509CertificatesTmp = createCertificates(caCertString);
                } catch (Exception e) {
                    LogUtils.e(TAG, "checkServerTrusted error. " + e.getLocalizedMessage());
                }

                Map<String, X509Certificate> issuerCertificateUniqueMap = new HashMap<>();
                int index;
                try {
                    if (x509CertificatesTmp.length <= 0 || x509Certificates.length <= 0) {
                        throw new CertificateException("bad param");
                    }
                    // dumpShakeHandCerts(x509Certificates);
                    // 将ca证书内容导入到map中备用
                    for (X509Certificate certificate : x509CertificatesTmp) {
                        issuerCertificateUniqueMap.put(
                                certificate.getSubjectDN().toString(), certificate);
                    }

                    X509Certificate serverDeepestCert;
                    X509Certificate checkTmp;
                    // 如果是证书链
                    if (x509Certificates.length > 1) {
                        // 正序
                        if (x509Certificates[0]
                                .getIssuerDN()
                                .equals(x509Certificates[1].getSubjectDN())) {
                            serverDeepestCert = x509Certificates[x509Certificates.length - 1];
                            checkTmp =
                                    issuerCertificateUniqueMap.get(
                                            serverDeepestCert.getIssuerDN().toString());
                            if (null == checkTmp) {
                                throw new CertificateException(
                                        "checkServerTrusted: Do not find the currect issuer form ca cert to check the server cert");
                            }

                            // 正序校验服务器发来的证书链
                            for (index = x509Certificates.length - 1; index >= 0; index--) {
                                x509Certificates[index].verify(checkTmp.getPublicKey());
                                checkTmp = x509Certificates[index];
                            }
                        } else {
                            // 反之认为是倒序
                            serverDeepestCert = x509Certificates[0];
                            checkTmp =
                                    issuerCertificateUniqueMap.get(
                                            serverDeepestCert.getIssuerDN().toString());
                            if (null == checkTmp) {
                                throw new CertificateException(
                                        "checkServerTrusted: Do not find the currect issuer form ca cert to check the server cert");
                            }

                            // 倒序校验
                            for (index = 0; index <= x509Certificates.length - 1; index++) {
                                x509Certificates[index].verify(checkTmp.getPublicKey());
                                checkTmp = x509Certificates[index];
                            }
                        }
                    } else {
                        // 非证书链的情况
                        serverDeepestCert = x509Certificates[0];
                        checkTmp =
                                issuerCertificateUniqueMap.get(
                                        serverDeepestCert.getIssuerDN().toString());
                        if (null == checkTmp) {
                            throw new CertificateException(
                                    "checkServerTrusted: Do not find the currect issuer form ca cert to check the server cert");
                        }
                        x509Certificates[0].verify(checkTmp.getPublicKey());
                    }

                    // 确定是否是一条证书链，暂时只检验root之后的一层
                    for (X509Certificate certificate : x509CertificatesTmp) {
                        if (certificate.getIssuerDN().equals(checkTmp.getSubjectDN())) {
                            if (!serverDeepestCert
                                    .getSubjectDN()
                                    .equals(certificate.getSubjectDN())) {
                                throw new CertificateException(
                                        "checkServerTrusted: The server cert chain is not the chain with trust store");
                            } else {
                                LogUtils.d(TAG, "it is the same chain with the trust store");
                                break;
                            }
                        }
                        // 没找到则认为checkTmp链的下一层没有子证书了
                    }
                } catch (NoSuchProviderException
                         | InvalidKeyException
                         | NoSuchAlgorithmException
                         | SignatureException e) {
                    LogUtils.e(TAG, "CertificateException error. " + e.getLocalizedMessage());
                    throw new CertificateException();
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    /**
     * Checks whether given X.509 certificate is self-signed.
     *
     * @return true if the certificate is self-signed
     */
    public static boolean isSelfSigned(X509Certificate cert) {
        try {
            cert.verify(cert.getPublicKey());
            return true;
        } catch (SignatureException | InvalidKeyException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 打印握手证书
     */
    private static String dumpShakeHandCerts(X509Certificate[] x509Certificates) {
        String result = "";
        try {
            result = certificateToString(x509Certificates);
        } catch (Exception e) {
            LogUtils.e(TAG, "dumpShakeHandCerts error. " + e.getLocalizedMessage());
        }
        return result;
    }

    /**
     * 将证书转换为string
     */
    private static String certificateToString(Certificate[] certs)
            throws IOException, CertificateEncodingException {
        ByteArrayOutputStream certFormat = new ByteArrayOutputStream();
        for (Certificate c : certs) {
            certFormat.write(BEGIN_CERT.getBytes(CHARSET_NAME));
            certFormat.write(Base64.encode(c.getEncoded(), 0));
            certFormat.write(END_CERT.getBytes(CHARSET_NAME));
        }
        return new String(certFormat.toByteArray(), CHARSET_NAME);
    }

    public static SSLSocketFactory getNoCheckSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{getTrustManager()}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static OkHttpClient getNoCheckOkHttpClient() {
        SSLSocketFactory ssl = getNoCheckSSLSocketFactory();
        X509TrustManager trustManager = getTrustManager();
        return new OkHttpClient.Builder()
                .connectTimeout(TimeUnit.SECONDS.toMillis(30), TimeUnit.SECONDS)
                .readTimeout(TimeUnit.SECONDS.toMillis(30), TimeUnit.SECONDS)
                .writeTimeout(TimeUnit.SECONDS.toMillis(30), TimeUnit.SECONDS)
                .sslSocketFactory(ssl, trustManager)
                .hostnameVerifier((hostname, session) -> true)
                .retryOnConnectionFailure(true)
                .build();
    }
}
