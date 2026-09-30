package com.example.data

import kotlinx.coroutines.flow.Flow

class ServiceRepository(private val dao: ServiceDao) {

    val allTickets: Flow<List<TicketEntity>> = dao.getAllTickets()
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    val technicians: Flow<List<UserEntity>> = dao.getTechnicians()
    val companyProfile: Flow<CompanyProfileEntity?> = dao.getCompanyProfile()

    suspend fun login(username: String, password: String): UserEntity? {
        return dao.login(username, password)
    }

    suspend fun getUserById(id: Long): UserEntity? {
        return dao.getUserById(id)
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        return dao.getUserByUsername(username)
    }

    suspend fun insertUser(user: UserEntity): Long {
        return dao.insertUser(user)
    }

    suspend fun insertUsers(users: List<UserEntity>) {
        dao.insertUsers(users)
    }

    suspend fun updateUser(user: UserEntity) {
        dao.updateUser(user)
    }

    suspend fun deleteUser(id: Long) {
        dao.deleteUserById(id)
    }

    suspend fun deleteNonAdminUsers() {
        dao.deleteNonAdminUsers()
    }

    fun getTicketsForTechnician(techId: Long): Flow<List<TicketEntity>> {
        return dao.getTicketsForTechnician(techId)
    }

    fun getTicketsForCustomer(phone: String, email: String): Flow<List<TicketEntity>> {
        return dao.getTicketsForCustomer(phone, email)
    }

    suspend fun getTicketByNumber(ticketNo: String): TicketEntity? {
        return dao.getTicketByNumber(ticketNo)
    }

    suspend fun getTicketById(id: Long): TicketEntity? {
        return dao.getTicketById(id)
    }

    suspend fun insertTicket(ticket: TicketEntity): Long {
        return dao.insertTicket(ticket)
    }

    suspend fun insertTickets(tickets: List<TicketEntity>) {
        dao.insertTickets(tickets)
    }

    suspend fun updateTicket(ticket: TicketEntity) {
        dao.updateTicket(ticket)
    }

    suspend fun deleteTicket(id: Long) {
        dao.deleteTicketById(id)
    }

    suspend fun deleteAllTickets() {
        dao.deleteAllTickets()
    }

    suspend fun deleteCompletedAndCancelledTickets() {
        dao.deleteCompletedAndCancelledTickets()
    }

    suspend fun saveCompanyProfile(profile: CompanyProfileEntity) {
        dao.insertCompanyProfile(profile)
    }

    suspend fun seedSampleDatabase() {
        AppDatabase.populateInitialData(dao)
    }

    suspend fun registerCustomer(fullName: String, phone: String, email: String): UserEntity {
        val newUser = UserEntity(
            username = email.ifEmpty { phone },
            password = "cust123",
            role = "CUSTOMER",
            fullName = fullName,
            phone = phone,
            email = email
        )
        val id = dao.insertUser(newUser)
        return newUser.copy(id = id)
    }
}
