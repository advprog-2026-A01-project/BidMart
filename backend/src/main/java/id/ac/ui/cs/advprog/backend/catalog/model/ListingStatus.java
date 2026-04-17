package id.ac.ui.cs.advprog.backend.catalog.model;

public enum ListingStatus {
    DRAFT,      // listing baru dibuat, penjual dapat mengedit
    ACTIVE,     // lelang sedang berlangsung, penawaran dapat diterima
    EXTENDED,   // waktu diperpanjang karena ada penawaran di menit akhir
    CLOSED,     // waktu lelang berakhir
    WON,        // harga cadangan terpenuhi, pemenang ditentukan
    UNSOLD      // harga cadangan tidak tercapai saat penutupan
}