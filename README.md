# ID Document Validation - Android

This project aims at providing **ID document verification** on the mobile device by processing the selected scanned image. Most of the mobile applications validate the ID document on the server side. 


# Validation Criteria - Completed

Document images can be pre-validated on the mobile device based on certain criteria listed below.

## Face Detection

Most of the ID documents comes with the person's photo printed on it. If that is the case, we can check for **single face** in the selected document to pass the face detection criteria.

# Validation Criteria - In Progress

## Image Resolution

The selected document image should be in a **preferred resolution** to pass this check. The user will be allowed to set the minimum and maximum required image resolution.

## Image Size

The user can set the minimum and maximum allowed size of the image.

## Image file format

The allowed image file formats should be configurable as per the requirements.

## Barcode, QR Code

If the ID document is expected to have a **barcode** or a **QR Code** embedded in it, this application checks the presence of a barcode or a QR Code.

# Validation Criteria - Future

## MRZ

Planning to add the **Machine Readable Zone** reader to check the documents like **Passport**.

## OCR with data validation

Offline **Optical Character Recognition** is planned to be added to this application and the extracted data will be validated for the sepcified format using Regex.
