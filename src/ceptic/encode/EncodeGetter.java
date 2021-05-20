package ceptic.encode;

import ceptic.encode.exceptions.UnknownEncodingException;

import java.util.*;

public class EncodeGetter {

    public static EncodeHandler get(String encodingString) throws UnknownEncodingException {
        if (encodingString == null || encodingString.isEmpty()) {
            return new EncodeHandler(new ArrayList<EncodeObject>() {
                {
                    add(EncodeType.none.getEncoder());
                }
            });
        }
        String[] encodings = encodingString.split(",");
        return get(Arrays.asList(encodings));
    }

    public static EncodeHandler get(List<String> encodings) throws UnknownEncodingException {
        List<EncodeObject> encoders = new ArrayList<>();
        Set<EncodeType> uniqueTypes = new HashSet<EncodeType>();
        for (String encoding : encodings) {
            EncodeType encodeType = EncodeType.fromValue(encoding);
            if (encodeType == null) {
                throw new UnknownEncodingException(String.format("EncodeType '%s' not recognized", encoding));
            }
            // if encoder type is none, just use this encoding type
            if (encodeType == EncodeType.none) {
                encoders = new ArrayList<>();
                encoders.add(encodeType.getEncoder());
                break;
            }
            // if encoder is unique, add to encoder list (and to unique types)
            if (!uniqueTypes.contains(encodeType)) {
                encoders.add(encodeType.getEncoder());
                uniqueTypes.add(encodeType);
            }
        }
        return new EncodeHandler(encoders);
    }

}
