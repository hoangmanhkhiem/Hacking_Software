
package com.trilead.ssh2.crypto;

import com.trilead.ssh2.compression.*;
import com.trilead.ssh2.crypto.cipher.*;
import com.trilead.ssh2.crypto.digest.*;
import com.trilead.ssh2.transport.*;


/**
 * CryptoWishList.
 * 
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: CryptoWishList.java,v 1.1 2007/10/15 12:49:56 cplattne Exp $
 */
public class CryptoWishList
{
	public String[] kexAlgorithms = KexManager.getDefaultKexAlgorithmList();
	public String[] serverHostKeyAlgorithms = KexManager.getDefaultServerHostkeyAlgorithmList();
	public String[] c2s_enc_algos = BlockCipherFactory.getDefaultCipherList();
	public String[] s2c_enc_algos = BlockCipherFactory.getDefaultCipherList();
	public String[] c2s_mac_algos = MessageMac.getMacs();
	public String[] s2c_mac_algos = MessageMac.getMacs();
	public String[] c2s_comp_algos = CompressionFactory
	.getDefaultCompressorList();
	public String[] s2c_comp_algos = CompressionFactory
	.getDefaultCompressorList();
}
