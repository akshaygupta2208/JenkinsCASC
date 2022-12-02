import jenkins
jenkins_server_url = "https://jenkins.softwaremathematics.com"
jenkins_url = jenkins.Jenkins(jenkins_server_url, username="admin", password="Strange@321")
job_list = [
    'Software-Mathematics/MMUAdminUI/mmu-admin',
    'Software-Mathematics/MMUAPI/aggregation-service',
    # 'Software-Mathematics/MMUAPI/apithf',
    'Software-Mathematics/MMUAPI/doses-service',
    'Software-Mathematics/MMUAPI/frequencymaster-service',
    'Software-Mathematics/MMUAPI/itemmaster-service',
    'Software-Mathematics/MMUAPI/labtests-serv',
    'Software-Mathematics/MMUAPI/medrequisitiongen-service',
    'Software-Mathematics/MMUAPI/mmuassociation-service',
    'Software-Mathematics/MMUAPI/mmucreation-service',
    'Software-Mathematics/MMUAPI/patient-service',
    'Software-Mathematics/MMUAPI/prescription-service',
    'Software-Mathematics/MMUAPI/project-service',
    'Software-Mathematics/MMUAPI/referal-service',
    'Software-Mathematics/MMUAPI/statusmaster-service',
    'Software-Mathematics/MMUAPI/stock-service',
    'Software-Mathematics/MMUAPI/testmaster-service',
    'Software-Mathematics/MMUAPI/transaction-service',
    'Software-Mathematics/MMUAPI/typemaster-service',
    'Software-Mathematics/MMUAPI/version-service',
    'Software-Mathematics/MMUAPI/visit-service',
    'Software-Mathematics/MMUAPI/vitals-service',
    'Software-Mathematics/MMUAPI/vitalsmaster-service',
    'Software-Mathematics/MMUAPI/warehousemaster-service',
    'Software-Mathematics/UserManagement/address-service',
    # 'Software-Mathematics/UserManagement/communicationrecord-service',
    # 'Software-Mathematics/UserManagement/communicationrecord-service-mongo',
    # 'Software-Mathematics/UserManagement/department-service',
    'Software-Mathematics/UserManagement/department-service-mongo',
    # 'Software-Mathematics/UserManagement/designation-service',
    'Software-Mathematics/UserManagement/designation-service-mongo',
    # 'Software-Mathematics/UserManagement/experience-service',
    # 'Software-Mathematics/UserManagement/experience-service-mongo',
    # 'Software-Mathematics/UserManagement/familydetails-service',
    'Software-Mathematics/UserManagement/familydetails-service-mongo',
    # 'Software-Mathematics/UserManagement/group-service',
    # 'Software-Mathematics/UserManagement/group-service-mongo',
    # 'Software-Mathematics/UserManagement/identities-service',
    # 'Software-Mathematics/UserManagement/kyc-service',
    # 'Software-Mathematics/UserManagement/kyc-service-mongo',
    # 'Software-Mathematics/UserManagement/personal-information-service',
    # 'Software-Mathematics/UserManagement/professionaldetails-service',
    # 'Software-Mathematics/UserManagement/professionaldetails-service-mongo',
    # 'Software-Mathematics/UserManagement/profile-service',
    'Software-Mathematics/UserManagement/profile-service-mongo',
    # 'Software-Mathematics/UserManagement/qualification-service',
    'Software-Mathematics/UserManagement/qualification-service-mongo',
    # 'Software-Mathematics/UserManagement/registration-login',
    # 'Software-Mathematics/UserManagement/resource-service',
    'Software-Mathematics/UserManagement/resource-service-mongo',
    # 'Software-Mathematics/UserManagement/role-service',
    'Software-Mathematics/UserManagement/role-service-mongo',
    # 'Software-Mathematics/UserManagement/scheme-service',
    'Software-Mathematics/UserManagement/scheme-service-mongo',
    # 'Software-Mathematics/UserManagement/statutoryrecords-service',
    # 'Software-Mathematics/UserManagement/statutoryrecords-service-mongo',
    'Software-Mathematics/UtilityAPIs/menu-service',
    # 'Software-Mathematics/UtilityAPIs/new-upload-service-mongo',
    'Software-Mathematics/workflow/camunda-service'
]
for job in job_list:
    jenkins_url.build_job(job)