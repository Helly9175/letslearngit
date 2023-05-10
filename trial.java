package com.yosatech.spring.Admin.Controllers;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.yosatech.spring.BusinessObjects.SendMail;
import com.yosatech.spring.ServerConstants.DOPermission;
import com.yosatech.spring.ServerConstants.DOSessionConstant;
import com.yosatech.spring.ServerConstants.MailRequestsConstants;
import com.yosatech.spring.Utils.CheckPermissionInProperty;
import com.yosatech.spring.Utils.DebugLog;
import com.yosatech.spring.Utils.EmailUtils;
import com.yosatech.spring.Utils.LoginToken;
import com.yosatech.spring.beans.EmployeeForm;
import com.yosatech.spring.dao.AddEmplyeeDao;
import com.yosatech.spring.dao.AllowanceDAO;

@Controller
public class AddEmplyeeController {

	@Autowired
	AddEmplyeeDao addEmplyeeDao;

	@Autowired
	AllowanceDAO allowanceDAO;
	
//	@Autowired
//	EmployeeService employeeService;

	@RequestMapping("/addEmployeeForm")
	public ModelAndView mymethod(EmployeeForm ef, HttpServletRequest request, Model model) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(DOSessionConstant.LOGINTOKEN) == null) {
			return new ModelAndView("redirect:/AdminLoginPage");
		} else {
			LoginToken s_LoginToken = (LoginToken) session.getAttribute(DOSessionConstant.LOGINTOKEN);
			if (addEmplyeeDao.checkSalaryConnection() && s_LoginToken.getAccessRole().equals("1")) {
				ef.setList(addEmplyeeDao.getAccessRole());
				ef.setDesignationlist(addEmplyeeDao.getDesignationDetail());
				ef.setTeamlist(addEmplyeeDao.getTeamDetail());
				DebugLog.log("Load Add Employee Form", DebugLog.LOG_INFO);
				return new ModelAndView("Admin/addEmployeeDetail", "command", ef);
			} else {
				DebugLog.log("System Error.Unable to connect database.", DebugLog.LOG_INFO);
				model.addAttribute("msg", "System Error.Unable to connect database.");
				return new ModelAndView("Admin/AdminErrorMessage");
			}
		}
	}

	@RequestMapping(value = "/insertEmployeeDetail", method = RequestMethod.POST)
	public ModelAndView saveEmployeeData(@ModelAttribute("m_EmployeeForm") EmployeeForm m_EmployeeForm,
			HttpServletRequest request, Model model) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(DOSessionConstant.LOGINTOKEN) == null) {
			DebugLog.log("Invalid session", DebugLog.LOG_INFO);
			return new ModelAndView("redirect:/AdminLoginPage");
		} else {
			LoginToken s_LoginToken = (LoginToken) session.getAttribute(DOSessionConstant.LOGINTOKEN);
			//String secretKey=employeeService.generateSecretKey();
			//m_EmployeeForm.setSecret(secretKey);
			int i = addEmplyeeDao.insertEmployeeData(m_EmployeeForm, s_LoginToken);
			if (i > 0) {
				DebugLog.log("Insert Employee Data In Employee Table", DebugLog.LOG_INFO);
				int empID = addEmplyeeDao.findMaxID();
				int loginEmployeeSalary = addEmplyeeDao.saveEmployeeSalary(m_EmployeeForm, empID);
				int loginAccInfo = 0;
				if (loginEmployeeSalary > 0) {
					DebugLog.log("Insert Employee Data In Employee Salary Detail Table", DebugLog.LOG_INFO);
					loginAccInfo = addEmplyeeDao.saveLeaveSummary(m_EmployeeForm, empID);
					loginAccInfo = addEmplyeeDao.saveLoginAccess(m_EmployeeForm, empID);
					if (m_EmployeeForm.getBankaccuntno() != null
							&& !m_EmployeeForm.getBankaccuntno().equalsIgnoreCase("")) {
						DebugLog.log("Bank Account Number Is Not Null", DebugLog.LOG_INFO);
						loginAccInfo = addEmplyeeDao.saveEmployeeBankDetail(m_EmployeeForm, empID);
						DebugLog.log("Insert Employee Data In Employee Bank Account Detail Table", DebugLog.LOG_INFO);
					}
				}
				if (loginAccInfo > 0) {
					OnSave(m_EmployeeForm, s_LoginToken);
					DebugLog.log("Employee has been registered successfully.", DebugLog.LOG_INFO);
					model.addAttribute("msg", "Employee has been registered successfully.");
					try {
						CheckPermissionInProperty permissionInProperty = new CheckPermissionInProperty();
						String hr1Email = permissionInProperty.getValue(DOPermission.HR1_EMAIL);
						String hr2Email = permissionInProperty.getValue(DOPermission.HR2_EMAIL);
						String companyHeadEmail = permissionInProperty.getValue(DOPermission.COMPANY_HEAD_EMAIL);
						String emailCC = hr1Email + "," + companyHeadEmail;			
						String subject=MailRequestsConstants.MESSAGE_SUB_NEW_EMPLOYEE_REGISTERED;
						String content=MailRequestsConstants.MESSAGE_BODY_NEW_EMPLOYEE_REGISTERED
								+ "</br> New employee name is " + m_EmployeeForm.getEmployeefirstname() + " "
								+ m_EmployeeForm.getEmployeelastname();
						
						Boolean mailStatus=EmailUtils.sendMail(hr1Email, hr2Email, emailCC, subject, content, null, null);
						if(mailStatus) {
							DebugLog.log("Sending email for the new employee registered successfully....", DebugLog.LOG_INFO);
						}else {
							DebugLog.log("Sending email for the new employee registered failed....", DebugLog.LOG_INFO);
						}
					} catch (Exception e) {
						// TODO: handle exception
						System.out.println("Error in sending mail notification for newly registered employee." + e);
						DebugLog.log("Error in sending mail notification for newly registered employee :" + e,
								DebugLog.LOG_INFO);
					}

					return new ModelAndView("Admin/AdminMsg");
				} else {
					DebugLog.log("Employee Login Detail Not Inserted", DebugLog.LOG_INFO);
					model.addAttribute("msg", "Employee Login Detail Not Inserted");
					return new ModelAndView("Admin/AdminMsg");
				}
			} else {
				DebugLog.log("Employee Detail Not Inserted", DebugLog.LOG_INFO);
				model.addAttribute("msg", "Employee Detail Not Inserted");
				return new ModelAndView("Admin/AdminMsg");
			}
		}
	}

	@RequestMapping("/selectEditEmplyee")
	public ModelAndView selectEditEmplyee(EmployeeForm ef, HttpServletRequest request, Model model) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(DOSessionConstant.LOGINTOKEN) == null) {
			DebugLog.log("Invalid Session", DebugLog.LOG_INFO);
			return new ModelAndView("redirect:/AdminLoginPage");
		} else {
			if (addEmplyeeDao.checkSalaryConnection() == false) {
				DebugLog.log("System Error.Unable to connect database.", DebugLog.LOG_INFO);
				model.addAttribute("msg", "System Error.Unable to connect database.");
				return new ModelAndView("Admin/AdminErrorMessage");
			}
			DebugLog.log("Select Employee Data", DebugLog.LOG_INFO);
			ef.setEmployeenamelist(addEmplyeeDao.getEmplyeeFullName());
			DebugLog.log("Load Select Edit Employee Detail", DebugLog.LOG_INFO);
			return new ModelAndView("Admin/selectEditEmplyeeDetail", "command", ef);
		}
	}

//	checkUserNameExistOrNot

	@RequestMapping(value = "/checkUserNameExistOrNot")
	public @ResponseBody int checkUserNameExistOrNot(HttpServletRequest request) {
		String uname = request.getParameter("uname");
		int i = addEmplyeeDao.CheckUsernameExistOrNot(uname);
		return i;
	}

	@RequestMapping(value = "/editEmployeeData")
	public ModelAndView edit(EmployeeForm ef, HttpServletRequest request, Model model) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(DOSessionConstant.LOGINTOKEN) == null) {
			DebugLog.log("Invalid Session", DebugLog.LOG_INFO);
			return new ModelAndView("redirect:/AdminLoginPage");
		} else {
			DebugLog.log("Select Edit Employee Data", DebugLog.LOG_INFO);
			int selecetEmpID = Integer.parseInt(request.getParameter("selecetEmpID"));
			model.addAttribute("accessRole", addEmplyeeDao.getAccessRole());
			model.addAttribute("designationDetail", addEmplyeeDao.getDesignationDetail());
			model.addAttribute("teamDetail", addEmplyeeDao.getTeamDetail());
			// model.addAttribute("EmployeeAllowance",addEmplyeeDao.getEmployeeAllowanceData(selecetEmpID));
			model.addAttribute("allowanceList", allowanceDAO.getAllowanceData(true));
			// model.addAttribute("empBankAcDetail",addEmplyeeDao.getEmployeeBankAccountDetail(selecetEmpID).get(0));
			// model.addAttribute("empSalaryDetail",addEmplyeeDao.getEmployeeSalaryDetail(selecetEmpID).get(0));
			ef = addEmplyeeDao.getEmployeeById(selecetEmpID);
			ef.setGrosspay(addEmplyeeDao.getEmployeeSalaryDetail(selecetEmpID));
			// ef.setUsername(addEmplyeeDao.getUserName(selecetEmpID));
			return new ModelAndView("Admin/editEmployeeDetail", "command", ef);
		}
	}

	@RequestMapping(value = "/updateEmployeeDetail", method = RequestMethod.POST)
	public ModelAndView editsave(@ModelAttribute("m_EmployeeForm") EmployeeForm m_EmployeeForm,
			HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(DOSessionConstant.LOGINTOKEN) == null) {
			DebugLog.log("Invalid Session", DebugLog.LOG_INFO);
			return new ModelAndView("redirect:/AdminLoginPage");
		} else {
			DebugLog.log("Update Employee Detail Function Loaded", DebugLog.LOG_INFO);
			LoginToken s_LoginToken = (LoginToken) session.getAttribute(DOSessionConstant.LOGINTOKEN);
			addEmplyeeDao.updateEmployeeData(m_EmployeeForm, s_LoginToken);
			return new ModelAndView("redirect:/selectEditEmplyee");
		}
	}

	// activeEmployee

	@RequestMapping("/activeEmployee")
	public ModelAndView activeEmployee(Model model, HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(DOSessionConstant.LOGINTOKEN) == null) {
			DebugLog.log("Invalid Session", DebugLog.LOG_INFO);
			return new ModelAndView("redirect:/AdminLoginPage");
		} else {
			DebugLog.log("Select All Employee Is Active Or Deactive", DebugLog.LOG_INFO);
			model.addAttribute("employeeDeactiveData", addEmplyeeDao.getEmplyeeDataIsDeavtive());
		}
		return new ModelAndView("Admin/ActiveEmployee");
	}

	// activeEmployeeAction
	@RequestMapping(value = "/activeEmployeeAction")
	public ModelAndView activeEmployeeAction(HttpServletRequest request, Model model) {
		int selecetEmpID = Integer.parseInt(request.getParameter("id"));
		int info = addEmplyeeDao.updateActiveAction(selecetEmpID);
		if (info > 0) {
			DebugLog.log("Employee Activate Success....", DebugLog.LOG_INFO);
			model.addAttribute("msg", "Employee Activate Success....");
			try {
				// get employee data
				EmployeeForm emp = addEmplyeeDao.getEmployeeById(selecetEmpID);
				
				CheckPermissionInProperty permissionInProperty = new CheckPermissionInProperty();
				String hr1Email = permissionInProperty.getValue(DOPermission.HR1_EMAIL);
				String hr2Email = permissionInProperty.getValue(DOPermission.HR2_EMAIL);
				String companyHeadEmail = permissionInProperty.getValue(DOPermission.COMPANY_HEAD_EMAIL);
				String emailCC = hr1Email + "," + companyHeadEmail;
				String mailSubject=MailRequestsConstants.MESSAGE_SUB_EMPLOYEE_ACTIVATED;
				String mailContent = MailRequestsConstants.MESSAGE_BODY_EMPLOYEE_ACTIVATED;
				mailContent += "</br>Employee Name:" + emp.getEmployeefirstname() + " " + emp.getEmployeelastname();	
					
				
				Boolean mailStatus=EmailUtils.sendMail(hr1Email, hr2Email, emailCC, mailSubject, mailContent, null, null);
				if(mailStatus) {
					DebugLog.log("Sending email for the employee deactivate successfully....", DebugLog.LOG_INFO);
				}else {
					DebugLog.log("Sending email for the employee deactivate failed....", DebugLog.LOG_INFO);
				}
				
			} catch (Exception e) {
				System.out.println("Error in sending mail for employee active:" + e);
				DebugLog.log("Error in sending mail for employee active: " + e, DebugLog.LOG_INFO);
			}

		} else {
			DebugLog.log("Employee Not Activate ....", DebugLog.LOG_INFO);
			model.addAttribute("msg", "Employee Not Activate ....");
		}
		model.addAttribute("employeeDeactiveData", addEmplyeeDao.getEmplyeeDataIsDeavtive());
		return new ModelAndView("Admin/ActiveEmployee");
	}

	@RequestMapping(value = "/deactiveEmployeeAction")
	public ModelAndView deactiveEmployeeAction(HttpServletRequest request, Model model) {
		int selecetEmpID = Integer.parseInt(request.getParameter("id"));
		int info = addEmplyeeDao.updateDeactiveAction(selecetEmpID);
		if (info > 0) {
			DebugLog.log("Employee Deactivate Success....", DebugLog.LOG_INFO);
			model.addAttribute("msg", "Employee Deactivate Success....");
			try {
				// get deactive employee data
				EmployeeForm emp = addEmplyeeDao.getEmployeeById(selecetEmpID);
				
				//set required value to send mail
				CheckPermissionInProperty permissionInProperty = new CheckPermissionInProperty();				
				String hr1Email = permissionInProperty.getValue(DOPermission.HR1_EMAIL);
				String hr2Email = permissionInProperty.getValue(DOPermission.HR2_EMAIL);
				String companyHeadEmail = permissionInProperty.getValue(DOPermission.COMPANY_HEAD_EMAIL);
				String emailCC = hr1Email + "," + companyHeadEmail;
				String mailSubject=MailRequestsConstants.MESSAGE_SUB_EMPLOYEE_DEACTIVATED;
				String mailContent = MailRequestsConstants.MESSAGE_BODY_EMPLOYEE_DEACTIVATED;
				mailContent += "</br>Employee Name:" + emp.getEmployeefirstname() + " " + emp.getEmployeelastname();
				
				Boolean mailStatus=EmailUtils.sendMail(hr1Email, hr2Email, emailCC, mailSubject, mailContent, null, null);
				if(mailStatus) {
					DebugLog.log("Sending email for the employee deactivate successfully....", DebugLog.LOG_INFO);
				}else {
					DebugLog.log("Sending email for the employee deactivate failed....", DebugLog.LOG_INFO);
				}
			} catch (Exception e) {
				System.out.println("Error in sending mail for employee deactive:" + e);
				DebugLog.log("Error in sending mail for employee deactive: " + e, DebugLog.LOG_INFO);
			}
		} else {
			DebugLog.log("Employee Not Deactivate ....", DebugLog.LOG_INFO);
			model.addAttribute("msg", "Employee Not Deactivate ....");
		}
		// return new ModelAndView("Admin/AdminErrorMessage");
		model.addAttribute("employeeDeactiveData", addEmplyeeDao.getEmplyeeDataIsDeavtive());
		return new ModelAndView("Admin/ActiveEmployee");
	}

	public void OnSave(EmployeeForm m_EmployeeForm, LoginToken m_LoginToken) {
		try {
			SendMail mail = new SendMail();
			mail.setM_MailFrom(m_LoginToken.getEmailID());
			mail.setM_MailTo(m_EmployeeForm.getEmailid());
			mail.setM_MailToBCC(m_EmployeeForm.getEmailid());

			mail.setM_MailSubject(MailRequestsConstants.MESSAGE_SUB_HR_NEW_EMP);
			String mailContent = MailRequestsConstants.MESSAGE_BODY_HR_NEW_EMP;
			mailContent = mailContent + "<br>" + "Usercode : = " + m_EmployeeForm.getUsername() + ", <br>"
					+ "Password : = Nuesoft11 <br>"
					+ "We recommend you to change the password after first login with this password <br><br><br> "
					+ "!! Welcome to yosatech !!";

			DebugLog.log("Start Mail Sending", DebugLog.LOG_INFO);
			mail.setM_MailContent(mailContent);
			mail.sendMail();
			DebugLog.log("Mail is Send success", DebugLog.LOG_INFO);
		} catch (Exception e) {

			DebugLog.log(e, DebugLog.LOG_INFO);
		}

	}

}
