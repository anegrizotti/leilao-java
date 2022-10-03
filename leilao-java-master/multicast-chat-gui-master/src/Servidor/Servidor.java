package Servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import static Criptografia.ModuloAES.gerarChave;
import static Criptografia.ModuloAES.GerarVetorInicializacao;

public class Servidor {

    public static final String ip = "230.0.0.0";
    public static final Integer porta = 4321;

    public static void main(String[] args) {
        IniciaServidor();
    }

    private static void Criptografa() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchAlgorithmException, NoSuchAlgorithmException {
        //criação da chave simétrica
        SecretKey chave = gerarChave(128);
        
        //iniciando o vetor da criptografia
        IvParameterSpec ivParameterSpec = GerarVetorInicializacao();

        byte[] secretMessage = chave.getEncoded();
        byte[] ivBytes = ivParameterSpec.getIV();

        //definindo a criptografia RS-A
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        while (true) {
            ServerSocket serverSocket = new ServerSocket(3333);
            Socket socket = serverSocket.accept();
            
            //obj recebe as msg
            DataInputStream socketInput = new DataInputStream(socket.getInputStream());
            
            //obj envia as msg
            DataOutputStream socketOutput = new DataOutputStream(socket.getOutputStream());

            byte[] mensagem = null;

            //tamanho da mensagem recebida (chave publica do cliente)
            int length = socketInput.readInt();   
            
            //lendo a chave e passando para mensagem
            if (length > 0) {
                mensagem = new byte[length];
                socketInput.readFully(mensagem, 0, mensagem.length); 
            }

            //criando a chave publica
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(mensagem);
            
            //gerando a chave publica
            PublicKey pubkey = keyFactory.generatePublic(publicKeySpec);

            //objeto de criptografia
            Cipher encryptCipher;
            
            //define criptografia rsa
            encryptCipher = Cipher.getInstance("RSA");
            
            //criptografa
            encryptCipher.init(Cipher.ENCRYPT_MODE, pubkey);

            //criptografa com a chave publica do cliente a chave simetrica do servidor
            byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessage);

            //enviando a mensagem criptografada
            socketOutput.writeInt(encryptedMessageBytes.length);
            socketOutput.write(encryptedMessageBytes);

            socketOutput.flush();

            //repete o processo de cima porem agora criptografa o vetor da criptografia
            byte[] ivEncryptedBytes = encryptCipher.doFinal(ivBytes);

            socketOutput.writeInt(ivEncryptedBytes.length);
            socketOutput.write(ivEncryptedBytes);

            socketOutput.flush();

            socketInput.close();
            socket.close();
            serverSocket.close();
        }
    }

    public static void RecebeMensagem() throws IOException {
        byte[] buffer = new byte[1024];
        MulticastSocket socket = new MulticastSocket(porta);
        InetAddress group = InetAddress.getByName(ip);
        socket.joinGroup(group);
        while (true) {
            System.out.println("Esperando Mensagens...");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
        }
    }

    public static void IniciaServidor() {
        try {
            Criptografa();
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            RecebeMensagem();
        } catch (IOException ex) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
