package com.goldenhour.main;

import com.goldenhour.ui.LoginUI;

import com.goldenhour.dataload.DataLoad;
import com.goldenhour.service.autoemail.AutoEmail;


public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome to Golden Hour System!");

        DataLoad.loadAllData();

        AutoEmail.startDailyScheduler();

        LoginUI.start();
    }
}
