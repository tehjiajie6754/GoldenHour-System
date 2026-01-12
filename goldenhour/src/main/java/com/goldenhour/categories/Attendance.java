package com.goldenhour.categories;

public class Attendance {
    private String employeeId;
    private String employeeName;
    private String date;
    private String clockInTime;
    private String clockOutTime;
    private double hoursWorked;
    private String outletCode;

    public Attendance() {}

    // Constructor for creating new attendance record (clock in - 5 parameters)
    public Attendance(String employeeId, String employeeName, String date, String clockInTime, String outletCode) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.date = date;
        this.clockInTime = clockInTime;
        this.clockOutTime = null;
        this.hoursWorked = 0.0;
        this.outletCode = outletCode;
    }

    // Full constructor (7 parameters - for loading from CSV)
    public Attendance(String employeeId, String employeeName, String date, String clockInTime, String clockOutTime, double hoursWorked, String outletCode) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.date = date;
        this.clockInTime = clockInTime;
        this.clockOutTime = clockOutTime;
        this.hoursWorked = hoursWorked;
        this.outletCode = outletCode;
    }

    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getDate() { return date; }
    public String getClockInTime() { return clockInTime; }
    public String getClockOutTime() { return clockOutTime; }
    public double getHoursWorked() { return hoursWorked; }
    public String getOutletCode() { return outletCode; }

    public void setClockOutTime(String clockOutTime) { this.clockOutTime = clockOutTime; }
    public void setHoursWorked(double hoursWorked) { this.hoursWorked = hoursWorked; }
    public void setDate(String date) { this.date = date; }
    public void setClockInTime(String clockInTime) { this.clockInTime = clockInTime; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setOutletCode(String outletCode) { this.outletCode = outletCode; }
    

    public String toCSV() {
        return String.join(",", employeeId, employeeName, date, 
                          clockInTime, (clockOutTime != null ? clockOutTime : ""), 
                          String.valueOf(hoursWorked), (outletCode != null ? outletCode : ""));
    }

    public static Attendance fromCSV(String line) {
        
        String[] data = line.split(",");
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i].trim();
        }
        String clockOut = data[4].isEmpty() ? null : data[4];
        String outlet = (data.length > 6 && !data[6].isEmpty()) ? data[6] : null;
        return new Attendance(data[0], data[1], data[2], data[3], clockOut, 
                            Double.parseDouble(data[5]), outlet);
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | Outlet: %s | Clock In: %s | Clock Out: %s | Hours: %.1f",
                employeeId, employeeName, date, (outletCode != null ? outletCode : "---"),
                clockInTime, (clockOutTime != null ? clockOutTime : "---"), hoursWorked);
    }
}

