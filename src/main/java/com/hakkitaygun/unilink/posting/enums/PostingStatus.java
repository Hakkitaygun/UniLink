package com.hakkitaygun.unilink.posting.enums;

public enum PostingStatus {
    OPEN,       // akışta görünür, başvuru alır
    CLOSED,     // süresi doldu: akıştan düşer, "Başvuru kapandı" yazar
    CANCELLED   // sahibi iptal etti
}