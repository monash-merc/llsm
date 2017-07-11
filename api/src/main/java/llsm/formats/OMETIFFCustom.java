package llsm.formats;

import java.io.IOException;

import io.scif.Plane;
import io.scif.FormatException;
import io.scif.formats.TIFFFormat;
import io.scif.formats.tiff.IFD;
import ome.xml.meta.OMEXMLMetadata;

/**
 * Custom OMETIFF Format
 * 
 * Just overrides the TIFFFormat Writer to inject simple OMEXML
 * metadata into the IMAGE_DESCRIPTION field of the first IFD.
 */
public class OMETIFFCustom extends TIFFFormat {

    public static class Writer extends TIFFFormat.Writer {
        private OMEXMLMetadata omexml;

        public OMEXMLMetadata getOMEXMLMeta() {
            return this.omexml;
        }
        public void setOMEXMLMeta(OMEXMLMetadata omexml) {
            this.omexml = omexml;
        }

        @Override
        public void savePlane(int imageIndex, long planeIndex, Plane plane, IFD ifd, long[] planeMin, long[] planeMax) throws IOException, FormatException {
            ifd.putIFDValue(IFD.IMAGE_DESCRIPTION, omexml.dumpXML());

            super.savePlane(imageIndex, planeIndex, plane, ifd, planeMin, planeMax);
        }
    }

}