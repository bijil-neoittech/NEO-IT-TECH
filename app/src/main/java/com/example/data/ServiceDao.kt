package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {

    // Users
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'TECHNICIAN' ORDER BY fullName ASC")
    fun getTechnicians(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("DELETE FROM users WHERE role != 'ADMIN'")
    suspend fun deleteNonAdminUsers()

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    // Tickets
    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE assignedTechnicianId = :techId ORDER BY createdAt DESC")
    fun getTicketsForTechnician(techId: Long): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE customerPhone = :phone OR customerEmail = :email ORDER BY createdAt DESC")
    fun getTicketsForCustomer(phone: String, email: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE ticketNumber = :ticketNo LIMIT 1")
    suspend fun getTicketByNumber(ticketNo: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun getTicketById(id: Long): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<TicketEntity>)

    @Update
    suspend fun updateTicket(ticket: TicketEntity)

    @Query("DELETE FROM tickets WHERE id = :id")
    suspend fun deleteTicketById(id: Long)

    @Query("DELETE FROM tickets")
    suspend fun deleteAllTickets()

    @Query("DELETE FROM tickets WHERE status = 'COMPLETED' OR status = 'CANCELLED'")
    suspend fun deleteCompletedAndCancelledTickets()

    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun getTicketCount(): Int

    // Company Profile
    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    fun getCompanyProfile(): Flow<CompanyProfileEntity?>

    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    suspend fun getCompanyProfileSync(): CompanyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanyProfile(profile: CompanyProfileEntity)
}
