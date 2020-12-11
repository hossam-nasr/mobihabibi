package edu.harvard.mobihabibi.img

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import org.apache.sanselan.ImageReadException
import org.apache.sanselan.ImageWriteException
import org.apache.sanselan.Sanselan
import org.apache.sanselan.common.IImageMetadata
import org.apache.sanselan.formats.jpeg.JpegImageMetadata
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter
import org.apache.sanselan.formats.tiff.TiffImageMetadata
import org.apache.sanselan.formats.tiff.constants.TagInfo
import org.apache.sanselan.formats.tiff.constants.TiffConstants
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory
import org.apache.sanselan.formats.tiff.write.TiffOutputField
import org.apache.sanselan.formats.tiff.write.TiffOutputSet
import java.io.*


class ImageEngine(private val context: Context) {
    fun saveImage(img: Bitmap): File? {
        val root = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).toString()
        val myDir = File(root)
        myDir.mkdirs()

        val fname = "${System.currentTimeMillis()}.jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        var successful = false
        try {
            val out = FileOutputStream(file)
            img.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            successful = true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(
            context, arrayOf(file.toString()), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }

        if (successful) {
            return file
        }
        return null
    }

    object ImageFilePath {
        var nopath = "Select Video Only"

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @SuppressLint("NewApi")
        fun getPath(context: Context, uri: Uri): String? {

            // check here to KITKAT or new version
            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return (Environment.getExternalStorageDirectory()
                            .toString() + "/"
                                + split[1])
                    }
                } else if (isDownloadsDocument(uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    return getDataColumn(context, contentUri, null, null)
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs =
                        arrayOf(split[1])
                    return getDataColumn(
                        context, contentUri, selection,
                        selectionArgs
                    )
                }
            } else if ("content".equals(uri.getScheme(), ignoreCase = true)) {

                // Return the remote address
                return if (isGooglePhotosUri(uri)) uri.getLastPathSegment() else getDataColumn(
                    context,
                    uri,
                    null,
                    null
                )
            } else if ("file".equals(uri.getScheme(), ignoreCase = true)) {
                return uri.path
            }
            return nopath
        }

        /**
         * Get the value of the data column for this Uri. This is <span id="IL_AD2" class="IL_AD">useful</span> for MediaStore Uris, and other file-based
         * ContentProviders.
         *
         * @param context
         * The context.
         * @param uri
         * The Uri to query.
         * @param selection
         * (Optional) Filter used in the query.
         * @param selectionArgs
         * (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        fun getDataColumn(
            context: Context, uri: Uri?,
            selection: String?, selectionArgs: Array<String>?
        ): String {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)
            try {
                cursor = uri?.let {
                    context.contentResolver.query(
                        it, projection,
                        selection, selectionArgs, null
                    )
                }
                if (cursor != null && cursor.moveToFirst()) {
                    val index: Int = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return nopath
        }

        /**
         * @param uri
         * The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri
                .authority
        }

        /**
         * @param uri
         * The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri
                .authority
        }

        /**
         * @param uri
         * The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri
                .authority
        }

        /**
         * @param uri
         * The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        private fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri
                .authority
        }
    }

    fun copyExifData(
        sourceFile: File,
        destFile: File,
        excludedFields: List<TagInfo?>?
    ): Boolean {
        val tempFileName = "${System.currentTimeMillis()}TEMP.jpg"
        var tempFile: File? = null
        var tempStream: OutputStream? = null
        try {
            val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString()
            val myDir = File(root)
            myDir.mkdirs()
            tempFile = File(myDir, tempFileName)

            val sourceSet: TiffOutputSet? =
                getSanselanOutputSet(sourceFile, TiffConstants.DEFAULT_TIFF_BYTE_ORDER)
            var destSet: TiffOutputSet? = null
            if (sourceSet != null) {
                destSet = getSanselanOutputSet(destFile, sourceSet.byteOrder)
            }

            // If the EXIF data endianess of the source and destination files
            // differ then fail. This only happens if the source and
            // destination images were created on different devices. It's
            // technically possible to copy this data by changing the byte
            // order of the data, but handling this case is outside the scope
            // of this implementation
            if (sourceSet?.byteOrder !== destSet?.byteOrder) return false
            destSet?.getOrCreateExifDirectory()

            // Go through the source directories
            val sourceDirectories: List<*> = sourceSet!!.getDirectories()
            for (i in sourceDirectories.indices) {
                val sourceDirectory: TiffOutputDirectory =
                    sourceDirectories[i] as TiffOutputDirectory
                val destinationDirectory: TiffOutputDirectory =
                    getOrCreateExifDirectory(destSet, sourceDirectory)
                        ?: continue  // failed to create

                // Loop the fields
                val sourceFields: List<*> = sourceDirectory.fields
                for (j in sourceFields.indices) {
                    // Get the source field
                    val sourceField: TiffOutputField = sourceFields[j] as TiffOutputField

                    // Check exclusion list
                    if (excludedFields != null && excludedFields.contains(sourceField.tagInfo)) {
                        destinationDirectory.removeField(sourceField.tagInfo)
                        continue
                    }

                    // Remove any existing field
                    destinationDirectory.removeField(sourceField.tagInfo)

                    // Add field
                    destinationDirectory.add(sourceField)
                }
            }

            tempStream = BufferedOutputStream(FileOutputStream(tempFile))
            ExifRewriter().updateExifMetadataLossless(destFile, tempStream, destSet)
            tempStream.close()

            // Replace file
            if (destFile.delete()) {
                tempFile.renameTo(destFile)
            }

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(
                context, arrayOf(tempFile.toString(), destFile.toString()), null
            ) { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
            }
            return true
        } catch (exception: ImageReadException) {
            exception.printStackTrace()
        } catch (exception: ImageWriteException) {
            exception.printStackTrace()
        } catch (exception: IOException) {
            exception.printStackTrace()
        } finally {
            if (tempStream != null) {
                try {
                    tempStream.close()
                } catch (e: IOException) {
                }
            }
            if (tempFile != null) {
                if (tempFile.exists()) tempFile.delete()
            }
        }
        return false
    }

    private fun getSanselanOutputSet(
        jpegImageFile: File,
        defaultByteOrder: Int
    ): TiffOutputSet? {
        var exif: TiffImageMetadata? = null
        var outputSet: TiffOutputSet? = null
        val metadata: IImageMetadata? = Sanselan.getMetadata(jpegImageFile)
        val jpegMetadata: JpegImageMetadata? = metadata as JpegImageMetadata?
        exif = jpegMetadata?.exif
        if (exif != null) {
            outputSet = exif.outputSet
        }

        // If JPEG file contains no EXIF metadata, create an empty set
        // of EXIF metadata. Otherwise, use existing EXIF metadata to
        // keep all other existing tags
        if (outputSet == null) outputSet =
            TiffOutputSet(exif?.contents?.header?.byteOrder ?: defaultByteOrder)
        return outputSet
    }

    private fun getOrCreateExifDirectory(
        outputSet: TiffOutputSet?,
        outputDirectory: TiffOutputDirectory
    ): TiffOutputDirectory? {
        var result: TiffOutputDirectory? = outputSet?.findDirectory(outputDirectory.type)
        if (result != null) return result
        result = TiffOutputDirectory(outputDirectory.type)
        try {
            outputSet?.addDirectory(result)
        } catch (e: ImageWriteException) {
            return null
        }
        return result
    }
}