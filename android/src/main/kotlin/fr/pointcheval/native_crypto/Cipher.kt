package fr.pointcheval.native_crypto

import java.lang.Exception
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.CipherOutputStream
import javax.crypto.CipherInputStream
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.util.Arrays;


enum class CipherAlgorithm(val spec: String) {
    AES("AES"),
    BlowFish("BLOWFISH")
}

enum class BlockCipherMode(val instance: String) {
    ECB("ECB"),
    CBC("CBC"),
    CFB("CFB"),
    GCM("GCM"),
    CGM("CGM"),
}

enum class Padding(val instance: String) {
    PKCS5("PKCS5Padding"),
    None("NoPadding")
}

class CipherParameters(private val mode: BlockCipherMode, private val padding: Padding) {
    override fun toString(): String {
        return mode.instance + "/" + padding.instance
    }
}

class Cipher {

    fun getCipherAlgorithm(dartAlgorithm: String) : CipherAlgorithm {
        return when (dartAlgorithm) {
            "aes" -> CipherAlgorithm.AES
            "blowfish" -> CipherAlgorithm.BlowFish
            else -> CipherAlgorithm.AES
        }
    }

    fun getInstance(mode : String, padding : String) : CipherParameters {
        val m = when (mode) {
            "ecb" -> BlockCipherMode.ECB
            "cbc" -> BlockCipherMode.CBC
            "cfb" -> BlockCipherMode.CFB
            "gcm" -> BlockCipherMode.GCM
            "cgm" -> BlockCipherMode.CGM
            else -> throw Exception()
        }
        val p = when (padding) {
            "pkcs5" -> Padding.PKCS5
            else -> Padding.None
        }
        return CipherParameters(m,p)
    }

    fun encrypt(data: ByteArray, key: ByteArray, algorithm: String, mode: String, padding: String) : List<ByteArray> {
        val algo = getCipherAlgorithm(algorithm)
        val params = getInstance(mode, padding)

        val keySpecification = algo.spec + "/" + params.toString()

        val mac = Hash().digest(key + data)
        val sk: SecretKey = SecretKeySpec(key, algo.spec)

        val cipher = Cipher.getInstance(keySpecification)
        cipher.init(Cipher.ENCRYPT_MODE, sk)

        val encryptedBytes = cipher.doFinal(mac + data)
        val iv = cipher.iv

        return listOf(encryptedBytes, iv);
    }

    fun decrypt(payload: Collection<ByteArray>, key: ByteArray, algorithm: String, mode: String, padding: String) : ByteArray? {
        val algo = getCipherAlgorithm(algorithm)
        val params = getInstance(mode, padding)

        val keySpecification = algo.spec + "/" + params.toString()

        val sk: SecretKey = SecretKeySpec(key, algo.spec)
        val cipher = Cipher.getInstance(keySpecification);
        val iv = payload.last();
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, sk, ivSpec);

        val decryptedBytes = cipher.doFinal(payload.first());

        val mac = decryptedBytes.copyOfRange(0, 32)
        val decryptedContent : ByteArray = decryptedBytes.copyOfRange(32, decryptedBytes.size)
        val verificationMac = Hash().digest(key + decryptedContent)

        return if (mac.contentEquals(verificationMac)) {
            decryptedContent
        } else {
            null;
        }
    }
    suspend fun encryptFile(inputFilePath:String, outputFilePath: String,algorithm: String,key: ByteArray, mode: String, padding: String) : ByteArray? {
        val algo = getCipherAlgorithm(algorithm);
        val params = getInstance(mode,padding);
        val keySpecification = algo.spec + "/" + params.toString();
        val sk = SecretKeySpec(key, algo.spec);
        val cipher = Cipher.getInstance(keySpecification);
        cipher.init(Cipher.ENCRYPT_MODE,sk);
        var len: Int;
        val buffer: ByteArray = ByteArray(8192);
        val inputFile = FileInputStream(inputFilePath);
        val outputFile = FileOutputStream(outputFilePath);
        val encryptedStream = CipherOutputStream(outputFile,cipher);
        while(true){
            len = inputFile.read(buffer);
            if(len > 0){
                encryptedStream.write(buffer,0,len);
            } else {
                break;
            }
        }
        encryptedStream.flush();
        encryptedStream.close();
        inputFile.close();
        return if (File(outputFilePath).exists() && cipher.iv != null){
            cipher.iv;
        } else {
            null;
        }
    }
    suspend fun decryptFile(inputFilePath: String, outputFilePath: String, algorithm: String, key: ByteArray, iv: ByteArray, mode: String, padding: String):Boolean {
        val algo = getCipherAlgorithm(algorithm);
        val params = getInstance(mode,padding);
        val keySpecification = algo.spec + "/" + params.toString();
        val sk = SecretKeySpec(key, algo.spec);
        val ivSpec = IvParameterSpec(iv);
        val cipher = Cipher.getInstance(keySpecification);
        var len: Int;
        val ibuffer: ByteArray? = ByteArray(8192);
        var obuffer: ByteArray?;
        cipher.init(Cipher.DECRYPT_MODE, sk, ivSpec);
        val outputFile = FileOutputStream(outputFilePath);
        val inputFile = FileInputStream(inputFilePath);
        var decryptedStream = CipherOutputStream(outputFile,cipher)
        while (true) {
            len = inputFile.read(ibuffer);
            if(len > 0){
                decryptedStream.write(ibuffer,0, len);
            } else {
                break;
            }
        }
        decryptedStream.flush();
        decryptedStream.close();
        inputFile.close();
        return if (File(outputFilePath).exists()){
            true;
        } else {
            false;
        }
    }
}