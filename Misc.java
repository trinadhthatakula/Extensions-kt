public class Misc{

   private void upiPayment(String amount, String note, String name, String upiId) {
        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .build();
        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);
        Intent intent = Intent.createChooser(upiPayIntent,"Pay Using");
        if(intent.resolveActivity(getPackageManager())!=null)
            startActivityForResult(intent, 101);
        else Toast.makeText(this, "No UPI app found, please install one to continue", Toast.LENGTH_SHORT).show();
    }


}
