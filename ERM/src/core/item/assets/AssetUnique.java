package core.item.assets;

//import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
 import org.apache.log4j.Logger;

import org.json.simple.JSONObject;

import com.google.common.primitives.Bytes;
//import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import controller.Controller;
import core.account.Account;
import core.crypto.Base58;
import core.transaction.Transaction;

public class AssetUnique extends AssetCls {
	
	private static final int TYPE_ID = AssetCls.UNIQUE;

	public AssetUnique(byte[] typeBytes, Account creator, String name, byte[] icon, byte[] image, String description)
	{
		super(typeBytes, creator, name, icon, image, description);
	}
	public AssetUnique(int props, Account creator, String name, byte[] icon, byte[] image, String description)
	{
		this(new byte[]{(byte)TYPE_ID, (byte)props}, creator, name, icon, image, description);
	}
	public AssetUnique(Account creator, String name, byte[] icon, byte[] image, String description)
	{
		this(new byte[]{(byte)TYPE_ID, (byte)0}, creator, name, icon, image, description);
	}

	//GETTERS/SETTERS
	public String getItemSubType() { return "unique"; }

	//PARSE
	// includeReference - TRUE only for store in local DB
	public static AssetUnique parse(byte[] data, boolean includeReference) throws Exception
	{	

		// READ TYPE
		byte[] typeBytes = Arrays.copyOfRange(data, 0, TYPE_LENGTH);
		int position = TYPE_LENGTH;
	
		//READ CREATOR
		byte[] creatorBytes = Arrays.copyOfRange(data, position, position + CREATOR_LENGTH);
		Account creator = new Account(Base58.encode(creatorBytes));
		position += CREATOR_LENGTH;
		
		//READ NAME
		//byte[] nameLengthBytes = Arrays.copyOfRange(data, position, position + NAME_SIZE_LENGTH);
		//int nameLength = Ints.fromByteArray(nameLengthBytes);
		//position += NAME_SIZE_LENGTH;
		int nameLength = Byte.toUnsignedInt(data[position]);
		position ++;
		
		if(nameLength < 1 || nameLength > MAX_NAME_LENGTH)
		{
			throw new Exception("Invalid name length");
		}
		
		byte[] nameBytes = Arrays.copyOfRange(data, position, position + nameLength);
		String name = new String(nameBytes, StandardCharsets.UTF_8);
		position += nameLength;

		//READ ICON
		byte[] iconLengthBytes = Arrays.copyOfRange(data, position, position + ICON_SIZE_LENGTH);
		int iconLength = Ints.fromBytes( (byte)0, (byte)0, iconLengthBytes[0], iconLengthBytes[1]);
		position += ICON_SIZE_LENGTH;
		
		if(iconLength > MAX_ICON_LENGTH)
		{
			throw new Exception("Invalid icon length");
		}
		
		byte[] icon = Arrays.copyOfRange(data, position, position + iconLength);
		position += iconLength;

		//READ IMAGE
		byte[] imageLengthBytes = Arrays.copyOfRange(data, position, position + IMAGE_SIZE_LENGTH);
		int imageLength = Ints.fromByteArray(imageLengthBytes);
		position += IMAGE_SIZE_LENGTH;
		
		if(imageLength > MAX_IMAGE_LENGTH)
		{
			throw new Exception("Invalid image length");
		}
		
		byte[] image = Arrays.copyOfRange(data, position, position + imageLength);
		position += imageLength;

		//READ DESCRIPTION
		byte[] descriptionLengthBytes = Arrays.copyOfRange(data, position, position + DESCRIPTION_SIZE_LENGTH);
		int descriptionLength = Ints.fromByteArray(descriptionLengthBytes);
		position += DESCRIPTION_SIZE_LENGTH;
		
		if(descriptionLength > 4000)
		{
			throw new Exception("Invalid description length");
		}
		
		byte[] descriptionBytes = Arrays.copyOfRange(data, position, position + descriptionLength);
		String description = new String(descriptionBytes, StandardCharsets.UTF_8);
		position += descriptionLength;
				
		//RETURN
		AssetUnique statement = new AssetUnique(typeBytes, creator, name, icon, image, description);

		if (includeReference)
		{
			//READ REFERENCE
			byte[] reference = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
			statement.setReference(reference);
		}
		return statement;
	}
		
	//OTHER
		
}
