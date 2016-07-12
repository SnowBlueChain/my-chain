package core.item.statuses;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;

import core.account.Account;
import core.crypto.Base58;

public class Status extends StatusCls {
	
	private static final int TYPE_ID = StatusCls.STATUS;

	public Status(Account creator, String name, byte[] icon, byte[] image, String description)
	{
		super(TYPE_ID, creator, name, icon, image, description);
	}
	public Status(byte[] typeBytes, Account creator, String name, byte[] icon, byte[] image, String description)
	{
		super(typeBytes, creator, name, icon, image, description);
	}

	//GETTERS/SETTERS

	public String getItemSubType() { return "status"; }

	//PARSE
	// includeReference - TRUE only for store in local DB
	public static Status parse(byte[] data, boolean includeReference) throws Exception
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
			throw new Exception("Invalid name length " + nameLength );
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
		
		byte[] reference = null;
		if (includeReference)
		{
			//READ REFERENCE
			reference = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
			position += REFERENCE_LENGTH;
		}
		
		//RETURN
		Status status = new Status(typeBytes, creator, name, icon, image, description);
		if (includeReference)
		{
			status.setReference(reference);
		}

		return status;
	}
	
}
