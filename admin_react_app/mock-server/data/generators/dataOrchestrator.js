const UserGenerator = require('./userGenerator');
const ParkingLotGenerator = require('./parkingLotGenerator');
const SessionGenerator = require('./sessionGenerator');
const logger = require('../../utils/logger');
const mockDataStore = require('../mockData');

class DataOrchestrator {
  constructor() {
    this.userGenerator = new UserGenerator();
    this.parkingLotGenerator = new ParkingLotGenerator();
    this.sessionGenerator = new SessionGenerator();
    this.generatedData = {
      superAdmins: [],
      admins: [],
      users: [],
      parkingLots: [],
      sessions: [],
      payments: [],
      adminLotAssignments: new Map()
    };
  }

  // Generate all data according to requirements
  async generateAllData() {
    logger.data('Starting comprehensive data generation');
    
    try {
      // Generate user data first
      await this.generateAllUserData();
      
      // Generate parking lot infrastructure
      await this.generateParkingLotData();
      
      // Assign parking lots to admins
      this.assignParkingLotsToAdmins(this.generatedData.parkingLots);
      
      // Generate 3-month session and payment data
      await this.generateSessionAndPaymentData();
      
      // Store all data in mock data store
      this.storeAllGeneratedData();
      
      logger.data('Complete data generation finished successfully', {
        superAdmins: this.generatedData.superAdmins.length,
        admins: this.generatedData.admins.length,
        users: this.generatedData.users.length,
        parkingLots: this.generatedData.parkingLots.length,
        sessions: this.generatedData.sessions.length,
        payments: this.generatedData.payments.length,
        totalUsers: this.generatedData.superAdmins.length + this.generatedData.admins.length + this.generatedData.users.length
      });

      return this.generatedData;

    } catch (error) {
      logger.dataError('Error generating complete data', {
        error: error.message,
        stack: error.stack
      });
      throw error;
    }
  }

  // Generate all user data according to requirements
  async generateAllUserData() {
    logger.data('Starting comprehensive user data generation');
    
    try {
      // Generate 2 Super Admin accounts
      await this.generateSuperAdmins();
      
      // Generate 15 Admin accounts (will assign lots later)
      await this.generateAdmins();
      
      // Generate 500+ regular user accounts
      await this.generateRegularUsers();
      
      // Create admin-to-parking-lot assignments (will be done after parking lots are generated)
      this.prepareAdminLotAssignments();
      
      logger.data('User data generation completed successfully', {
        superAdmins: this.generatedData.superAdmins.length,
        admins: this.generatedData.admins.length,
        users: this.generatedData.users.length,
        totalUsers: this.generatedData.superAdmins.length + this.generatedData.admins.length + this.generatedData.users.length
      });

      return this.generatedData;

    } catch (error) {
      logger.dataError('Error generating user data', {
        error: error.message,
        stack: error.stack
      });
      throw error;
    }
  }

  // Generate parking lot infrastructure data
  async generateParkingLotData() {
    logger.data('Starting parking lot infrastructure generation');
    
    try {
      // Generate 25 parking lots as per requirements
      this.generatedData.parkingLots = this.parkingLotGenerator.generateMultipleParkingLots(25);
      
      logger.data('Parking lot generation completed successfully', {
        totalLots: this.generatedData.parkingLots.length,
        totalCapacity: this.generatedData.parkingLots.reduce((sum, lot) => sum + lot.total_slots, 0),
        capacityRange: this.getParkingLotCapacityStats(),
        cityDistribution: this.getParkingLotCityDistribution()
      });

      return this.generatedData.parkingLots;

    } catch (error) {
      logger.dataError('Error generating parking lot data', {
        error: error.message,
        stack: error.stack
      });
      throw error;
    }
  }

  // Generate 3-month session and payment data
  async generateSessionAndPaymentData() {
    logger.data('Starting 3-month session and payment data generation');
    
    try {
      // Generate sessions using all parking lots and users
      this.generatedData.sessions = this.sessionGenerator.generate3MonthSessions(
        this.generatedData.parkingLots,
        this.generatedData.users
      );
      
      // Generate payment records from sessions
      this.generatedData.payments = this.generatePaymentRecords(this.generatedData.sessions);
      
      // Get session statistics
      const sessionStats = this.sessionGenerator.getSessionStatistics();
      
      logger.data('Session and payment generation completed successfully', {
        totalSessions: this.generatedData.sessions.length,
        totalPayments: this.generatedData.payments.length,
        activeSessions: sessionStats.activeSessions,
        completedSessions: sessionStats.completedSessions,
        vehicleTypeDistribution: sessionStats.vehicleTypeDistribution,
        paymentStatusDistribution: sessionStats.paymentStatusDistribution,
        revenueStats: sessionStats.revenueStats
      });

      return {
        sessions: this.generatedData.sessions,
        payments: this.generatedData.payments
      };

    } catch (error) {
      logger.dataError('Error generating session and payment data', {
        error: error.message,
        stack: error.stack
      });
      throw error;
    }
  }

  // Generate payment records from sessions
  generatePaymentRecords(sessions) {
    logger.data('Generating payment records from sessions');
    
    const payments = [];
    let paymentIdCounter = 1;
    
    sessions.forEach(session => {
      // Only create payment records for sessions with amounts
      if (session.total_amount > 0) {
        const payment = {
          payment_id: paymentIdCounter++,
          session_id: session.session_id,
          ticket_id: session.ticket_id,
          user_id: session.user_id,
          parkinglot_id: session.parkinglot_id,
          amount: session.total_amount,
          payment_method: session.payment_method,
          payment_status: session.payment_status,
          transaction_id: this.generateTransactionId(session.payment_method, paymentIdCounter),
          payment_timestamp: session.payment_timestamp,
          
          // Payment gateway information
          gateway_response: this.generateGatewayResponse(session.payment_status),
          processing_fee: this.calculateProcessingFee(session.total_amount, session.payment_method),
          net_amount: session.total_amount - this.calculateProcessingFee(session.total_amount, session.payment_method),
          
          // Refund information
          refund_status: 'none',
          refund_amount: 0,
          refund_reason: null,
          
          // Audit information
          created_at: session.start_time,
          updated_at: session.payment_timestamp || session.updated_at,
          
          // Additional metadata
          currency: 'INR',
          payment_description: `Parking fee for ${session.vehicle_reg_no}`,
          merchant_reference: session.ticket_id,
          
          // Reconciliation information
          settlement_date: session.payment_status === 'success' ? 
            this.calculateSettlementDate(session.payment_timestamp) : null,
          settlement_status: session.payment_status === 'success' ? 'settled' : 'pending'
        };
        
        payments.push(payment);
      }
    });
    
    logger.data('Payment records generated', {
      totalPayments: payments.length,
      paymentMethods: this.getPaymentMethodDistribution(payments),
      statusDistribution: this.getPaymentStatusDistribution(payments)
    });
    
    return payments;
  }

  // Generate transaction ID based on payment method
  generateTransactionId(paymentMethod, paymentId) {
    const prefixes = {
      upi: 'UPI',
      card: 'CARD',
      wallet: 'WALLET',
      cash: 'CASH'
    };
    
    const prefix = prefixes[paymentMethod] || 'TXN';
    const timestamp = Date.now().toString().slice(-8);
    const id = String(paymentId).padStart(6, '0');
    
    return `${prefix}${timestamp}${id}`;
  }

  // Generate gateway response based on payment status
  generateGatewayResponse(paymentStatus) {
    const responses = {
      success: {
        code: '200',
        message: 'Transaction successful',
        gateway_txn_id: `GW${Date.now()}${Math.random().toString(36).substr(2, 6).toUpperCase()}`
      },
      pending: {
        code: '102',
        message: 'Transaction pending',
        gateway_txn_id: `GW${Date.now()}${Math.random().toString(36).substr(2, 6).toUpperCase()}`
      },
      failed: {
        code: '400',
        message: this.randomChoice([
          'Insufficient funds',
          'Transaction declined',
          'Network timeout',
          'Invalid card details',
          'Transaction limit exceeded'
        ]),
        gateway_txn_id: null
      }
    };
    
    return responses[paymentStatus];
  }

  // Calculate processing fee based on amount and payment method
  calculateProcessingFee(amount, paymentMethod) {
    const feeStructure = {
      upi: 0, // No fee for UPI
      card: amount * 0.02, // 2% for cards
      wallet: amount * 0.01, // 1% for wallets
      cash: 0 // No fee for cash
    };
    
    const fee = feeStructure[paymentMethod] || 0;
    return Math.round(fee * 100) / 100; // Round to 2 decimal places
  }

  // Calculate settlement date (T+1 for most payment methods)
  calculateSettlementDate(paymentTimestamp) {
    if (!paymentTimestamp) return null;
    
    const paymentDate = new Date(paymentTimestamp);
    const settlementDate = new Date(paymentDate);
    settlementDate.setDate(paymentDate.getDate() + 1); // T+1 settlement
    
    return settlementDate.toISOString();
  }

  // Get payment method distribution
  getPaymentMethodDistribution(payments) {
    const distribution = {};
    payments.forEach(payment => {
      distribution[payment.payment_method] = (distribution[payment.payment_method] || 0) + 1;
    });
    return distribution;
  }

  // Get payment status distribution for payments
  getPaymentStatusDistribution(payments) {
    const distribution = {};
    payments.forEach(payment => {
      distribution[payment.payment_status] = (distribution[payment.payment_status] || 0) + 1;
    });
    return distribution;
  }

  // Random choice helper
  randomChoice(array) {
    return array[Math.floor(Math.random() * array.length)];
  }

  // Generate 2 Super Admin accounts
  async generateSuperAdmins() {
    logger.data('Generating Super Admin accounts');
    
    for (let i = 0; i < 2; i++) {
      const superAdmin = await this.userGenerator.generateSuperAdmin(i);
      
      // Add specific customizations for super admins
      if (i === 0) {
        // First super admin - CONSTANT credentials as requested
        superAdmin.user_id = 1; // Ensure user_id is always 1
        superAdmin.user_email = 'superadmin@parking.com'; // Constant email
        superAdmin.user_name = 'Super Admin';
        superAdmin.profile.designation = 'General Manager';
        // Password is 'password123' (set in userGenerator based on index)
      } else {
        // Second super admin
        superAdmin.profile.designation = 'Operations Director';
        superAdmin.user_email = 'ops.director@parkingadmin.com';
      }
      
      superAdmin.super_admin_details.security_clearance = 'level_5';
      
      this.generatedData.superAdmins.push(superAdmin);
      
      logger.data('Generated Super Admin', {
        userId: superAdmin.user_id,
        name: superAdmin.user_name,
        email: superAdmin.user_email,
        designation: superAdmin.profile.designation
      });
    }
  }

  // Generate 15 Admin accounts
  async generateAdmins() {
    logger.data('Generating Admin accounts');
    
    for (let i = 0; i < 15; i++) {
      const admin = await this.userGenerator.generateAdmin();
      
      // Ensure the first admin has deterministic credentials for easy login
      if (i === 0) {
        admin.user_email = 'admin10@parking.com';
        admin.user_name = 'Admin User';
        admin.password_hash = await this.userGenerator.generatePasswordHash('password123');
      }
      
      // Customize admin details
      admin.profile.employee_id = `ADM${String(i + 1).padStart(3, '0')}`;
      admin.admin_details.shift_timings = this.generateShiftTimings(i);
      
      // Set reporting manager (first super admin)
      if (this.generatedData.superAdmins.length > 0) {
        admin.profile.reporting_manager = {
          user_id: this.generatedData.superAdmins[0].user_id,
          name: this.generatedData.superAdmins[0].user_name,
          email: this.generatedData.superAdmins[0].user_email
        };
      }
      
      this.generatedData.admins.push(admin);
      
      logger.data('Generated Admin', {
        userId: admin.user_id,
        name: admin.user_name,
        email: admin.user_email,
        employeeId: admin.profile.employee_id,
        designation: admin.profile.designation
      });
    }
  }

  // Generate 500+ regular user accounts
  async generateRegularUsers() {
    logger.data('Generating regular user accounts');
    
    const targetUsers = 520; // Generate 520 users (500+ requirement)
    const batchSize = 50; // Generate in batches for better performance
    
    for (let batch = 0; batch < Math.ceil(targetUsers / batchSize); batch++) {
      const batchStart = batch * batchSize;
      const batchEnd = Math.min(batchStart + batchSize, targetUsers);
      const batchSize_actual = batchEnd - batchStart;
      
      logger.data(`Generating user batch ${batch + 1}`, {
        batchStart: batchStart + 1,
        batchEnd,
        batchSize: batchSize_actual
      });
      
      const batchPromises = [];
      for (let i = batchStart; i < batchEnd; i++) {
        batchPromises.push(this.userGenerator.generateRegularUser());
      }
      
      const batchUsers = await Promise.all(batchPromises);
      this.generatedData.users.push(...batchUsers);
      
      // Log progress every 100 users
      if ((batch + 1) % 2 === 0 || batch === Math.ceil(targetUsers / batchSize) - 1) {
        logger.data('User generation progress', {
          completed: this.generatedData.users.length,
          target: targetUsers,
          percentage: Math.round((this.generatedData.users.length / targetUsers) * 100)
        });
      }
    }
    
    logger.data('Regular user generation completed', {
      totalUsers: this.generatedData.users.length
    });
  }

  // Generate shift timings for admins
  generateShiftTimings(adminIndex) {
    const shifts = [
      { start: '06:00', end: '14:00', name: 'Morning Shift' },
      { start: '08:00', end: '16:00', name: 'Day Shift' },
      { start: '10:00', end: '18:00', name: 'Regular Shift' },
      { start: '14:00', end: '22:00', name: 'Evening Shift' },
      { start: '16:00', end: '24:00', name: 'Night Shift' }
    ];
    
    const shift = shifts[adminIndex % shifts.length];
    
    return {
      start_time: shift.start,
      end_time: shift.end,
      shift_name: shift.name,
      days: adminIndex < 12 ? 
        ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'] : // Most admins work 6 days
        ['monday', 'tuesday', 'wednesday', 'thursday', 'friday'] // Some work 5 days
    };
  }

  // Prepare admin-lot assignments (will be completed after parking lots are generated)
  prepareAdminLotAssignments() {
    logger.data('Preparing admin-lot assignment structure');
    
    // Initialize assignment map for each admin
    this.generatedData.admins.forEach(admin => {
      this.generatedData.adminLotAssignments.set(admin.user_id, {
        admin_id: admin.user_id,
        admin_name: admin.user_name,
        admin_email: admin.user_email,
        assigned_lots: [], // Will be populated when parking lots are generated
        assignment_date: admin.created_at,
        is_active: true,
        permissions: admin.admin_details.permissions
      });
    });
  }

  // Assign parking lots to admins (called after parking lots are generated)
  assignParkingLotsToAdmins(parkingLots) {
    logger.data('Assigning parking lots to admins', {
      totalLots: parkingLots.length,
      totalAdmins: this.generatedData.admins.length
    });

    // Ensure each parking lot has at least one admin
    // Some admins can manage multiple lots, but each lot must have at least one admin
    
    const lotAssignments = new Map();
    const adminWorkload = new Map();
    
    // Initialize admin workload tracking
    this.generatedData.admins.forEach(admin => {
      adminWorkload.set(admin.user_id, 0);
    });
    
    // Assign primary admin to each lot
    parkingLots.forEach((lot, index) => {
      const adminIndex = index % this.generatedData.admins.length;
      const admin = this.generatedData.admins[adminIndex];
      
      if (!lotAssignments.has(lot.parkinglot_id)) {
        lotAssignments.set(lot.parkinglot_id, []);
      }
      
      lotAssignments.get(lot.parkinglot_id).push({
        admin_id: admin.user_id,
        admin_name: admin.user_name,
        admin_email: admin.user_email,
        assignment_type: 'primary',
        assigned_date: new Date().toISOString()
      });
      
      adminWorkload.set(admin.user_id, adminWorkload.get(admin.user_id) + 1);
    });
    
    // Add secondary admins to some lots (larger lots or high-traffic areas)
    const highCapacityLots = parkingLots.filter(lot => lot.total_slots > 60);
    
    highCapacityLots.forEach(lot => {
      // 50% chance of getting a secondary admin
      if (Math.random() > 0.5) {
        // Find admin with lowest workload
        let minWorkload = Math.min(...adminWorkload.values());
        let availableAdmins = this.generatedData.admins.filter(admin => 
          adminWorkload.get(admin.user_id) === minWorkload &&
          !lotAssignments.get(lot.parkinglot_id).some(assignment => assignment.admin_id === admin.user_id)
        );
        
        if (availableAdmins.length > 0) {
          const secondaryAdmin = availableAdmins[Math.floor(Math.random() * availableAdmins.length)];
          
          lotAssignments.get(lot.parkinglot_id).push({
            admin_id: secondaryAdmin.user_id,
            admin_name: secondaryAdmin.user_name,
            admin_email: secondaryAdmin.user_email,
            assignment_type: 'secondary',
            assigned_date: new Date().toISOString()
          });
          
          adminWorkload.set(secondaryAdmin.user_id, adminWorkload.get(secondaryAdmin.user_id) + 0.5);
        }
      }
    });
    
    // Update admin records with their assigned lots
    this.generatedData.admins.forEach(admin => {
      const assignedLots = [];
      
      for (const [lotId, assignments] of lotAssignments.entries()) {
        const adminAssignment = assignments.find(a => a.admin_id === admin.user_id);
        if (adminAssignment) {
          const lot = parkingLots.find(l => l.parkinglot_id === lotId);
          assignedLots.push({
            parkinglot_id: lotId,
            parkinglot_name: lot.parkinglot_name,
            location: lot.location,
            assignment_type: adminAssignment.assignment_type,
            assigned_date: adminAssignment.assigned_date
          });
        }
      }
      
      admin.admin_details.assigned_lots = assignedLots;
      
      // Update assignment map
      if (this.generatedData.adminLotAssignments.has(admin.user_id)) {
        this.generatedData.adminLotAssignments.get(admin.user_id).assigned_lots = assignedLots;
      }
    });
    
    // Update super admin statistics
    this.generatedData.superAdmins.forEach(superAdmin => {
      superAdmin.statistics.admins_supervised = this.generatedData.admins.length;
    });
    
    logger.data('Parking lot assignments completed', {
      totalAssignments: Array.from(lotAssignments.values()).reduce((sum, assignments) => sum + assignments.length, 0),
      averageLotsPerAdmin: (Array.from(lotAssignments.values()).reduce((sum, assignments) => sum + assignments.length, 0) / this.generatedData.admins.length).toFixed(2),
      adminWorkloadDistribution: Array.from(adminWorkload.entries()).map(([adminId, workload]) => ({
        adminId,
        workload
      }))
    });
    
    return lotAssignments;
  }

  // Store generated data in mock data store
  storeGeneratedData() {
    logger.data('Storing generated user data in mock data store');
    
    // Store super admins
    this.generatedData.superAdmins.forEach(superAdmin => {
      mockDataStore.superAdmins.set(superAdmin.user_id, superAdmin);
    });
    
    // Store admins
    this.generatedData.admins.forEach(admin => {
      mockDataStore.admins.set(admin.user_id, admin);
    });
    
    // Store regular users
    this.generatedData.users.forEach(user => {
      mockDataStore.users.set(user.user_id, user);
    });
    
    // Store admin lot assignments
    for (const [adminId, assignment] of this.generatedData.adminLotAssignments.entries()) {
      mockDataStore.adminLotAssignments.set(adminId, assignment);
    }
    
    logger.data('User data stored successfully', {
      superAdminsStored: mockDataStore.superAdmins.size,
      adminsStored: mockDataStore.admins.size,
      usersStored: mockDataStore.users.size,
      assignmentsStored: mockDataStore.adminLotAssignments.size
    });
  }

  // Get generation statistics
  getGenerationStatistics() {
    return {
      superAdmins: {
        count: this.generatedData.superAdmins.length,
        roles: this.generatedData.superAdmins.map(sa => sa.profile.designation)
      },
      admins: {
        count: this.generatedData.admins.length,
        shiftDistribution: this.getShiftDistribution(),
        locationDistribution: this.getLocationDistribution()
      },
      users: {
        count: this.generatedData.users.length,
        genderDistribution: this.getGenderDistribution(),
        cityDistribution: this.getCityDistribution(),
        vehicleDistribution: this.getVehicleDistribution()
      },
      totalUsers: this.generatedData.superAdmins.length + this.generatedData.admins.length + this.generatedData.users.length
    };
  }

  // Get shift distribution for admins
  getShiftDistribution() {
    const distribution = {};
    this.generatedData.admins.forEach(admin => {
      const shiftName = admin.admin_details.shift_timings.shift_name;
      distribution[shiftName] = (distribution[shiftName] || 0) + 1;
    });
    return distribution;
  }

  // Get location distribution
  getLocationDistribution() {
    const distribution = {};
    [...this.generatedData.admins, ...this.generatedData.users].forEach(user => {
      const city = user.address_details.city;
      distribution[city] = (distribution[city] || 0) + 1;
    });
    return distribution;
  }

  // Get gender distribution
  getGenderDistribution() {
    const distribution = {};
    [...this.generatedData.admins, ...this.generatedData.users].forEach(user => {
      const gender = user.profile.gender;
      distribution[gender] = (distribution[gender] || 0) + 1;
    });
    return distribution;
  }

  // Get city distribution
  getCityDistribution() {
    const distribution = {};
    this.generatedData.users.forEach(user => {
      const city = user.address_details.city;
      distribution[city] = (distribution[city] || 0) + 1;
    });
    return distribution;
  }

  // Get vehicle distribution
  getVehicleDistribution() {
    const distribution = { car: 0, motorcycle: 0, total_vehicles: 0 };
    this.generatedData.users.forEach(user => {
      user.vehicle_info.forEach(vehicle => {
        distribution[vehicle.type]++;
        distribution.total_vehicles++;
      });
    });
    return distribution;
  }

  // Store all generated data in mock data store
  storeAllGeneratedData() {
    logger.data('Storing all generated data in mock data store');
    
    // Store super admins
    this.generatedData.superAdmins.forEach(superAdmin => {
      mockDataStore.superAdmins.set(superAdmin.user_id, superAdmin);
    });
    
    // Store admins
    this.generatedData.admins.forEach(admin => {
      mockDataStore.admins.set(admin.user_id, admin);
    });
    
    // Store regular users
    this.generatedData.users.forEach(user => {
      mockDataStore.users.set(user.user_id, user);
    });
    
    // Store parking lots
    this.generatedData.parkingLots.forEach(lot => {
      mockDataStore.parkingLots.set(lot.parkinglot_id, lot);
    });
    
    // Store sessions
    this.generatedData.sessions.forEach(session => {
      mockDataStore.sessions.set(session.session_id, session);
    });
    
    // Store payments
    this.generatedData.payments.forEach(payment => {
      mockDataStore.payments.set(payment.payment_id, payment);
    });
    
    // Store admin lot assignments
    for (const [adminId, assignment] of this.generatedData.adminLotAssignments.entries()) {
      mockDataStore.adminLotAssignments.set(adminId, assignment);
    }
    
    logger.data('All data stored successfully', {
      superAdminsStored: mockDataStore.superAdmins.size,
      adminsStored: mockDataStore.admins.size,
      usersStored: mockDataStore.users.size,
      parkingLotsStored: mockDataStore.parkingLots.size,
      sessionsStored: mockDataStore.sessions.size,
      paymentsStored: mockDataStore.payments.size,
      assignmentsStored: mockDataStore.adminLotAssignments.size
    });
  }

  // Get parking lot capacity statistics
  getParkingLotCapacityStats() {
    if (this.generatedData.parkingLots.length === 0) return null;
    
    const capacities = this.generatedData.parkingLots.map(lot => lot.total_slots);
    return {
      min: Math.min(...capacities),
      max: Math.max(...capacities),
      average: Math.round(capacities.reduce((sum, cap) => sum + cap, 0) / capacities.length),
      total: capacities.reduce((sum, cap) => sum + cap, 0)
    };
  }

  // Get parking lot city distribution
  getParkingLotCityDistribution() {
    const distribution = {};
    this.generatedData.parkingLots.forEach(lot => {
      const city = lot.location.city;
      distribution[city] = (distribution[city] || 0) + 1;
    });
    return distribution;
  }

  // Get comprehensive generation statistics
  getComprehensiveStatistics() {
    const sessionStats = this.sessionGenerator.getSessionStatistics();
    
    return {
      users: this.getGenerationStatistics(),
      parkingLots: {
        count: this.generatedData.parkingLots.length,
        capacityStats: this.getParkingLotCapacityStats(),
        cityDistribution: this.getParkingLotCityDistribution(),
        facilitiesDistribution: this.getFacilitiesDistribution(),
        pricingModelDistribution: this.getPricingModelDistribution()
      },
      sessions: sessionStats ? {
        totalSessions: sessionStats.totalSessions,
        activeSessions: sessionStats.activeSessions,
        completedSessions: sessionStats.completedSessions,
        vehicleTypeDistribution: sessionStats.vehicleTypeDistribution,
        paymentStatusDistribution: sessionStats.paymentStatusDistribution,
        durationStats: sessionStats.durationStats,
        revenueStats: sessionStats.revenueStats,
        peakHourAnalysis: sessionStats.peakHourAnalysis,
        dayTypeAnalysis: sessionStats.dayTypeAnalysis
      } : null,
      payments: {
        totalPayments: this.generatedData.payments.length,
        paymentMethods: this.getPaymentMethodDistribution(this.generatedData.payments),
        statusDistribution: this.getPaymentStatusDistribution(this.generatedData.payments),
        totalRevenue: this.getTotalRevenue(),
        processingFees: this.getTotalProcessingFees()
      },
      adminAssignments: {
        totalAssignments: this.generatedData.adminLotAssignments.size,
        averageLotsPerAdmin: this.getAverageLotsPerAdmin(),
        assignmentTypes: this.getAssignmentTypeDistribution()
      }
    };
  }

  // Get facilities distribution
  getFacilitiesDistribution() {
    const distribution = {};
    this.generatedData.parkingLots.forEach(lot => {
      lot.facilities.available_facilities.forEach(facility => {
        distribution[facility] = (distribution[facility] || 0) + 1;
      });
    });
    return distribution;
  }

  // Get pricing model distribution
  getPricingModelDistribution() {
    const distribution = {};
    this.generatedData.parkingLots.forEach(lot => {
      const modelType = lot.pricing_structure.model_type;
      distribution[modelType] = (distribution[modelType] || 0) + 1;
    });
    return distribution;
  }

  // Get average lots per admin
  getAverageLotsPerAdmin() {
    if (this.generatedData.admins.length === 0) return 0;
    
    const totalAssignments = Array.from(this.generatedData.adminLotAssignments.values())
      .reduce((sum, assignment) => sum + assignment.assigned_lots.length, 0);
    
    return (totalAssignments / this.generatedData.admins.length).toFixed(2);
  }

  // Get assignment type distribution
  getAssignmentTypeDistribution() {
    const distribution = { primary: 0, secondary: 0 };
    
    Array.from(this.generatedData.adminLotAssignments.values()).forEach(assignment => {
      assignment.assigned_lots.forEach(lot => {
        distribution[lot.assignment_type] = (distribution[lot.assignment_type] || 0) + 1;
      });
    });
    
    return distribution;
  }

  // Get total revenue from payments
  getTotalRevenue() {
    return this.generatedData.payments
      .filter(payment => payment.payment_status === 'success')
      .reduce((sum, payment) => sum + payment.amount, 0);
  }

  // Get total processing fees
  getTotalProcessingFees() {
    return this.generatedData.payments
      .reduce((sum, payment) => sum + payment.processing_fee, 0);
  }

  // Reset all generated data
  reset() {
    this.userGenerator.reset();
    this.parkingLotGenerator.reset();
    this.sessionGenerator.reset();
    this.generatedData = {
      superAdmins: [],
      admins: [],
      users: [],
      parkingLots: [],
      sessions: [],
      payments: [],
      adminLotAssignments: new Map()
    };
    logger.data('Data orchestrator reset completed');
  }
}

module.exports = DataOrchestrator;